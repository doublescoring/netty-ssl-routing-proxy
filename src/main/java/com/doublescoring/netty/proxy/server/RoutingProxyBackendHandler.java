package com.doublescoring.netty.proxy.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Proxy connection handler. Writes all data from the target server to the client channel.
 */
public class RoutingProxyBackendHandler extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(RoutingProxyBackendHandler.class);

	private final Channel inboundChannel;

	public RoutingProxyBackendHandler(Channel inboundChannel) {
		this.inboundChannel = Objects.requireNonNull(inboundChannel);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.read();
		ctx.write(Unpooled.EMPTY_BUFFER);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("Connection inactive: " + ctx.channel());
		RoutingProxyFrontendHandler.closeOnFlush(inboundChannel);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		inboundChannel.writeAndFlush(msg).addListener((ChannelFuture future) -> {
			if (future.isSuccess()) {
				ctx.channel().read();
			} else {
				future.channel().close();
			}
		});
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Exception caught", cause);
		RoutingProxyFrontendHandler.closeOnFlush(ctx.channel());
	}
}
