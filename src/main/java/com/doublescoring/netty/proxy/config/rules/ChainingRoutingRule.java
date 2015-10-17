package com.doublescoring.netty.proxy.config.rules;

import com.doublescoring.netty.proxy.config.RoutingContext;
import com.doublescoring.netty.proxy.config.RoutingRule;
import com.doublescoring.netty.proxy.config.RoutingTarget;

import java.util.Objects;
import java.util.Optional;

/**
 * Chaining routing rule. It proxies route method call to the delegates and
 * returns first non-empty result.
 */
public class ChainingRoutingRule implements RoutingRule {
	private final RoutingRule[] rules;

	public ChainingRoutingRule(RoutingRule ... rules) {
		this.rules = Objects.requireNonNull(rules);
	}

	@Override
	public Optional<RoutingTarget> route(RoutingContext context) {
		for (RoutingRule rule : rules) {
			Optional<RoutingTarget> target = rule.route(context);
			if (target.isPresent()) {
				return target;
			}
		}

		return Optional.empty();
	}
}
