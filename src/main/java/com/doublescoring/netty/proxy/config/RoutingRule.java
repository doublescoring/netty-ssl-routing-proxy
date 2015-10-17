package com.doublescoring.netty.proxy.config;

import java.util.Optional;

/**
 * Interface for the rule based routing.
 */
public interface RoutingRule {
	/**
	 * Returns optional with RoutingTarget. Returns Optional.empty() if the context passed could not be routed to
	 * any target.
	 */
	Optional<RoutingTarget> route(RoutingContext context);
}
