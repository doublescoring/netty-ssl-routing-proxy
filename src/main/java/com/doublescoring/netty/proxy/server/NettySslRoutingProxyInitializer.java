package com.doublescoring.netty.proxy.server;

import com.doublescoring.netty.proxy.config.NettySslRoutingProxyConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

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
		ch.pipeline()
				.addLast(config.getSslContextConfiguration().getSslContext().newHandler(ch.alloc()))
				.addLast(new LoggingHandler(NettySslRoutingProxyInitializer.class, LogLevel.DEBUG))
				.addLast(new SslInboundHandler(config))
				.addLast(new RoutingProxyFrontendHandler());
	}
}
