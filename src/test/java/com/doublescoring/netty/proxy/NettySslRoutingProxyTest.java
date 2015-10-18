package com.doublescoring.netty.proxy;

import com.doublescoring.netty.proxy.config.NettySslRoutingProxyConfig;
import com.doublescoring.netty.proxy.config.RoutingTarget;
import com.doublescoring.netty.proxy.config.rules.ChainingRoutingRule;
import com.doublescoring.netty.proxy.config.rules.ExplicitRoutingRule;
import com.doublescoring.netty.proxy.config.rules.IntermediateCertificateRoutingRule;
import com.doublescoring.netty.proxy.config.rules.X509SubjectContainsStringRoutingRule;
import com.doublescoring.netty.proxy.config.ssl.BouncyCastleSslKeyMaterialSource;
import com.doublescoring.netty.proxy.config.ssl.SslKeyMaterialSource;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;

public class NettySslRoutingProxyTest {
	private static final String LOCALHOST = "localhost";

	private EventLoopGroup bossGroup = new NioEventLoopGroup(10);
	private EventLoopGroup workerGroup = new NioEventLoopGroup();

	@BeforeClass
	public static void configureNetty() {
		ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
	}

	@After
	public void close() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}

	/**
	 * Client certificate authentication test.
	 */
	@Test(expected = SSLHandshakeException.class)
	public void testClientAuth() throws Exception {
		int targetServerPort = getRandomPort();
		SslKeyMaterialSource ca = new BouncyCastleSslKeyMaterialSource("ca.example.com");
		final NettySslRoutingProxyConfig config = TestNettySslRoutingProxyConfig.create()
				.withSslKeyMaterialSource(new BouncyCastleSslKeyMaterialSource("server.example.com", ca))
				.withRoutingRule(new ExplicitRoutingRule(new RoutingTarget(LOCALHOST, targetServerPort)));

		final String prefix = "<request>";
		final String marker = "<response>";

		try (AutoCloseable proxyServer = new TestSslRoutingProxyServer(config);
			 AutoCloseable targetServer = new TestConstantStringServer(targetServerPort, marker);
		) {
			SslKeyMaterialSource cert = new BouncyCastleSslKeyMaterialSource("client.example.com");
			final SslContext clientContext = SslContextBuilder.forClient()
					.keyManager(cert.getPrivateKey(), cert.getCertificateChain())
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

			final SimpleConsumingHandler handler = new SimpleConsumingHandler();
			new Bootstrap()
					.group(new NioEventLoopGroup())
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(clientContext.newHandler(ch.alloc(),
									config.getBindHost(), config.getBindPort()));
							ch.pipeline().addLast(new LoggingHandler(NettySslRoutingProxyTest.class, LogLevel.DEBUG));
							ch.pipeline().addLast(handler);
						}
					})
					.connect(config.getBindHost(), config.getBindPort())
					.sync()
					.channel()
					.writeAndFlush(Unpooled.copiedBuffer(prefix, CharsetUtil.UTF_8))
					.sync().channel()
					.read().closeFuture().sync();
		}
	}

	/**
	 * Tests routing based on X509 subject.
	 */
	@Test
	public void testX509SubjectRouting() throws Exception {
		SslKeyMaterialSource ca = new BouncyCastleSslKeyMaterialSource("ca.example.com");

		String firstSubject = "first.example.com";
		String secondSubject = "second.example.com";

		int targetServerPortFirst = getRandomPort();
		int targetServerPortSecond = getRandomPort();
		final NettySslRoutingProxyConfig config = TestNettySslRoutingProxyConfig.create()
				.withSslKeyMaterialSource(new BouncyCastleSslKeyMaterialSource("server.example.com", ca))
				.withTrustedCertificate(ca.getCertificateChain()[0])
				.withRoutingRule(
						new ChainingRoutingRule(
								new X509SubjectContainsStringRoutingRule(
										new RoutingTarget(LOCALHOST, targetServerPortFirst), "first"),
								new X509SubjectContainsStringRoutingRule(
										new RoutingTarget(LOCALHOST, targetServerPortSecond), "second")
						));

		final String prefix = "<request>";
		final String markerFirst = "<response-first>";
		final String markerSecond = "<response-second>";

		try (AutoCloseable proxyServer = new TestSslRoutingProxyServer(config);
			 AutoCloseable targetServerFirst = new TestConstantStringServer(targetServerPortFirst, markerFirst);
			 AutoCloseable targetServerSecond = new TestConstantStringServer(targetServerPortSecond, markerSecond)
		) {
			SslKeyMaterialSource certFirst = new BouncyCastleSslKeyMaterialSource(firstSubject, ca);
			final SslContext clientContextFirst = SslContextBuilder.forClient()
					.keyManager(certFirst.getPrivateKey(), certFirst.getCertificateChain())
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

			final SimpleConsumingHandler handlerFirst = new SimpleConsumingHandler();
			new Bootstrap()
					.group(new NioEventLoopGroup())
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(clientContextFirst.newHandler(ch.alloc(),
									config.getBindHost(), config.getBindPort()));
							ch.pipeline().addLast(new LoggingHandler(NettySslRoutingProxyTest.class, LogLevel.DEBUG));
							ch.pipeline().addLast(handlerFirst);
						}
					})
					.connect(config.getBindHost(), config.getBindPort())
					.sync()
					.channel()
					.writeAndFlush(Unpooled.copiedBuffer(prefix, CharsetUtil.UTF_8))
					.sync().channel()
					.read().closeFuture().sync();
			assertEquals(prefix + markerFirst, handlerFirst.getResult());

			SslKeyMaterialSource certSecond = new BouncyCastleSslKeyMaterialSource(secondSubject, ca);
			final SslContext clientContextSecond = SslContextBuilder.forClient()
					.keyManager(certSecond.getPrivateKey(), certSecond.getCertificateChain())
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

			final SimpleConsumingHandler handlerSecond = new SimpleConsumingHandler();
			new Bootstrap()
					.group(new NioEventLoopGroup())
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(clientContextSecond.newHandler(ch.alloc(),
									config.getBindHost(), config.getBindPort()));
							ch.pipeline().addLast(new LoggingHandler(NettySslRoutingProxyTest.class, LogLevel.DEBUG));
							ch.pipeline().addLast(handlerSecond);
						}
					})
					.connect(config.getBindHost(), config.getBindPort())
					.sync()
					.channel()
					.writeAndFlush(Unpooled.copiedBuffer(prefix, CharsetUtil.UTF_8))
					.sync().channel()
					.read().closeFuture().sync();
			assertEquals(prefix + markerSecond, handlerSecond.getResult());
		}
	}

	/**
	 * Tests routing based on intermediate certificate.
	 */
	@Test
	public void testIntermediateCertRouting() throws Exception {
		SslKeyMaterialSource ca = new BouncyCastleSslKeyMaterialSource("ca.example.com");
		SslKeyMaterialSource intermediateCa = new BouncyCastleSslKeyMaterialSource("test.ca.example.com", ca);

		String firstSubject = "first.example.com";
		String secondSubject = "second.example.com";

		int targetServerPortFirst = getRandomPort();
		int targetServerPortSecond = getRandomPort();
		final NettySslRoutingProxyConfig config = TestNettySslRoutingProxyConfig.create()
				.withSslKeyMaterialSource(new BouncyCastleSslKeyMaterialSource("server.example.com", ca))
				.withTrustedCertificate(ca.getCertificateChain()[0])
				.withTrustedCertificate(intermediateCa.getCertificateChain()[0])
				.withRoutingRule(
						new ChainingRoutingRule(
								new IntermediateCertificateRoutingRule(
										new RoutingTarget(LOCALHOST, targetServerPortFirst), "CN=test.ca.example.com"),
								new IntermediateCertificateRoutingRule(
										new RoutingTarget(LOCALHOST, targetServerPortSecond), "CN=ca.example.com")
						));

		final String prefix = "<request>";
		final String markerFirst = "<response-first>";
		final String markerSecond = "<response-second>";

		try (AutoCloseable proxyServer = new TestSslRoutingProxyServer(config);
			 AutoCloseable targetServerFirst = new TestConstantStringServer(targetServerPortFirst, markerFirst);
			 AutoCloseable targetServerSecond = new TestConstantStringServer(targetServerPortSecond, markerSecond)
		) {
			SslKeyMaterialSource certFirst = new BouncyCastleSslKeyMaterialSource(firstSubject, intermediateCa);
			final SslContext clientContextFirst = SslContextBuilder.forClient()
					.keyManager(certFirst.getPrivateKey(), certFirst.getCertificateChain())
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

			final SimpleConsumingHandler handlerFirst = new SimpleConsumingHandler();
			new Bootstrap()
					.group(new NioEventLoopGroup())
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(clientContextFirst.newHandler(ch.alloc(),
									config.getBindHost(), config.getBindPort()));
							ch.pipeline().addLast(new LoggingHandler(NettySslRoutingProxyTest.class, LogLevel.DEBUG));
							ch.pipeline().addLast(handlerFirst);
						}
					})
					.connect(config.getBindHost(), config.getBindPort())
					.sync()
					.channel()
					.writeAndFlush(Unpooled.copiedBuffer(prefix, CharsetUtil.UTF_8))
					.sync().channel()
					.read().closeFuture().sync();
			assertEquals(prefix + markerFirst, handlerFirst.getResult());

			SslKeyMaterialSource certSecond = new BouncyCastleSslKeyMaterialSource(secondSubject, ca);
			final SslContext clientContextSecond = SslContextBuilder.forClient()
					.keyManager(certSecond.getPrivateKey(), certSecond.getCertificateChain())
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();

			final SimpleConsumingHandler handlerSecond = new SimpleConsumingHandler();
			new Bootstrap()
					.group(new NioEventLoopGroup())
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(clientContextSecond.newHandler(ch.alloc(),
									config.getBindHost(), config.getBindPort()));
							ch.pipeline().addLast(new LoggingHandler(NettySslRoutingProxyTest.class, LogLevel.DEBUG));
							ch.pipeline().addLast(handlerSecond);
						}
					})
					.connect(config.getBindHost(), config.getBindPort())
					.sync()
					.channel()
					.writeAndFlush(Unpooled.copiedBuffer(prefix, CharsetUtil.UTF_8))
					.sync().channel()
					.read().closeFuture().sync();
			assertEquals(prefix + markerSecond, handlerSecond.getResult());
		}
	}

	private final class TestSslRoutingProxyServer implements AutoCloseable {
		private final Channel channel;

		public TestSslRoutingProxyServer(NettySslRoutingProxyConfig config) throws InterruptedException {
			channel = NettySslRoutingProxy.start(config, bossGroup, workerGroup);
		}

		public void close() throws Exception {
			channel.close().sync();
		}
	}

	/**
	 * Simple ECHO-linke test server. Response consist of incoming message and constant string
	 */
	private final class TestConstantStringServer implements AutoCloseable {
		private final Channel channel;

		public TestConstantStringServer(int port, final String constant) throws InterruptedException {
			channel = new ServerBootstrap()
					.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(TestConstantStringServer.class, LogLevel.DEBUG))
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(new LoggingHandler(TestConstantStringServer.class, LogLevel.DEBUG));
							ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
								@Override
								public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
									ctx.write(msg);
									ctx.writeAndFlush(Unpooled.copiedBuffer(constant, CharsetUtil.UTF_8))
											.addListener(ChannelFutureListener.CLOSE);
								}
							});
						}
					})
					.bind(LOCALHOST, port)
					.sync()
					.channel();
		}

		public void close() throws Exception {
			channel.close().sync();
		}
	}

	private int getRandomPort() throws IOException {
		return new ServerSocket(0).getLocalPort();
	}

	private final class SimpleConsumingHandler extends ChannelInboundHandlerAdapter {
		private StringBuffer buffer = new StringBuffer();

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ByteBuf buf = (ByteBuf) msg;
			try {
				buf.forEachByte(value -> {
					buffer.append((char) value);
					return true;
				});
			} finally {
				buf.release();
			}
		}

		public String getResult() {
			return buffer.toString();
		}
	}



}