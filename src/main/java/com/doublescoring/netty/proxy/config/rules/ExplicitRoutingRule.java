package com.doublescoring.netty.proxy.config.rules;

import com.doublescoring.netty.proxy.config.RoutingContext;
import com.doublescoring.netty.proxy.config.RoutingRule;
import com.doublescoring.netty.proxy.config.RoutingTarget;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Simples routing rule - routes all connections to the target explicitly specified.
 */
public class ExplicitRoutingRule implements RoutingRule {
	private final RoutingTarget target;

	@JsonCreator
	public ExplicitRoutingRule(@JsonProperty("target") RoutingTarget target) {
		this.target = Objects.requireNonNull(target);
	}

	public Optional<RoutingTarget> route(RoutingContext context) {
		return Optional.of(target);
	}

	public RoutingTarget getTarget() {
		return target;
	}
}
