package com.doublescoring.netty.proxy;

import com.doublescoring.netty.proxy.config.NettySslRoutingProxyConfig;
import com.doublescoring.netty.proxy.config.JsonNettySslRoutingProxyConfig;
import com.doublescoring.netty.proxy.server.NettySslRoutingProxyInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ssl routing proxy server entry point.
 */
public class NettySslRoutingProxy {
	private static final Logger logger = LoggerFactory.getLogger(NettySslRoutingProxy.class);

	public static Channel start(final NettySslRoutingProxyConfig config, EventLoopGroup bossGroup,
								EventLoopGroup workerGroup)
			throws InterruptedException {
		return new ServerBootstrap()
				.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(NettySslRoutingProxy.class, LogLevel.INFO))
				.childHandler(new NettySslRoutingProxyInitializer(config))
				.bind(config.getBindHost(), config.getBindPort())
				.sync()
				.channel();
	}

	public static void start(NettySslRoutingProxyConfig config) throws InterruptedException {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			start(config, bossGroup, workerGroup).closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		logger.info("Starting Netty SSL routing proxy");
		start(JsonNettySslRoutingProxyConfig.parse(args[0]));
		logger.info("Netty SSL routing proxy configured");
	}
}
