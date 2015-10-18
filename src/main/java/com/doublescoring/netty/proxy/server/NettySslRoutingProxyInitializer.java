package com.doublescoring.netty.proxy.server;

import com.doublescoring.netty.proxy.config.NettySslRoutingProxyConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import java.security.cert.X509Certificate;

/**
 * Ssl routing proxy initializer. Contains actual pipeline chain.
 */
public class NettySslRoutingProxyInitializer extends ChannelInitializer<SocketChannel> {
	private final NettySslRoutingProxyConfig config;

	public NettySslRoutingProxyInitializer(NettySslRoutingProxyConfig config) {
		this.config = config;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		SslContext sslContext = SslContextBuilder.forServer(config.getKeyMaterialSource().getPrivateKey(),
				config.getKeyMaterialSource().getCertificateChain())
				.sslProvider(SslProvider.JDK)
				.trustManager(config.getTrustedCertificates().toArray(new X509Certificate[]{}))
				.clientAuth(ClientAuth.REQUIRE)
				.build();
		ch.pipeline()
				.addLast(sslContext.newHandler(ch.alloc()))
				.addLast(new LoggingHandler(NettySslRoutingProxyInitializer.class, LogLevel.DEBUG))
				.addLast(new SslInboundHandler(config))
				.addLast(new RoutingProxyFrontendHandler());
	}
}
