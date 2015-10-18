package com.doublescoring.netty.proxy.server;

import com.doublescoring.netty.proxy.config.NettySslRoutingProxyConfig;
import com.doublescoring.netty.proxy.config.RoutingContext;
import com.doublescoring.netty.proxy.config.RoutingTarget;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * SSL inbound connections handlers. Executes client validation and assigns
 * routing target for the current connection.
 */
public class SslInboundHandler extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(SslInboundHandler.class);

	private final NettySslRoutingProxyConfig config;

	public SslInboundHandler(NettySslRoutingProxyConfig config) {
		this.config = config;
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		SslHandler sslHandler = Objects.requireNonNull(ctx.pipeline().get(SslHandler.class));
		sslHandler.handshakeFuture().addListener(future -> {
			if (future.isSuccess()) {
				RoutingContext context = new RoutingContext();
				context.setCertificateChain(sslHandler.engine().getSession().getPeerCertificateChain());

				Optional<RoutingTarget> target = config.getRoutingRule().route(context);
				if (target.isPresent()) {
					ctx.pipeline().get(RoutingProxyFrontendHandler.class).initProxyConnection(ctx, target.get());
				} else {
					logger.error("Unable to find target for routing context: " + context);
					ctx.close();
				}
			} else {
				logger.error("Handshake failure");
				ctx.close();
			}
		});
	}
}
