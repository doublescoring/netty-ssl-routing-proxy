package com.doublescoring.netty.proxy.server;

import com.doublescoring.netty.proxy.config.RoutingTarget;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Proxy connection handler. Writes all data from the client to the target server channel.
 */
public class RoutingProxyFrontendHandler extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(RoutingProxyFrontendHandler.class);

	private volatile Channel outboundChannel;

	/**
	 * Creates connection to the target server. Has to be called before channelRead.
	 */
	protected void initProxyConnection(ChannelHandlerContext ctx, RoutingTarget routingTarget)
			throws InterruptedException {
		final Channel inboundChannel = ctx.channel();
		Objects.requireNonNull(routingTarget);
		logger.debug("Creating proxy connection to " + routingTarget);

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(inboundChannel.eventLoop())
				.channel(ctx.channel().getClass())
				.handler(new RoutingProxyBackendHandler(inboundChannel))
				.option(ChannelOption.AUTO_READ, false);

		final ChannelFuture f = bootstrap.connect(routingTarget.getHost(), routingTarget.getPort());
		outboundChannel = f.channel();
		f.addListener(future -> {
			if (future.isSuccess()) {
				inboundChannel.read();
			} else {
				logger.error("Connection attemp has failed");
				inboundChannel.close();
			}
		});
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (Objects.requireNonNull(outboundChannel).isActive()) {
			outboundChannel.writeAndFlush(msg).addListener((ChannelFuture future) -> {
				 if (future.isSuccess()) {
					 ctx.channel().read();
				 } else {
					 future.channel().close();
				 }
			});
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Exception caught", cause);
		closeOnFlush(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}
	}

	static void closeOnFlush(Channel ch) {
		if (ch.isActive()) {
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
