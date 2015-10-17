package com.doublescoring.netty.proxy.config.rules;

import com.doublescoring.netty.proxy.config.RoutingContext;
import com.doublescoring.netty.proxy.config.RoutingRule;
import com.doublescoring.netty.proxy.config.RoutingTarget;

import java.util.Objects;
import java.util.Optional;

/**
 * Routes to the specified target if X509 certificate subject contains specified substring.
 */
public class X509SubjectContainsStringRoutingRule implements RoutingRule {
	private final RoutingTarget target;
	private final String pattern;

	public X509SubjectContainsStringRoutingRule(RoutingTarget target, String pattern) {
		this.target = Objects.requireNonNull(target);
		this.pattern = Objects.requireNonNull(pattern);
	}


	@Override
	public Optional<RoutingTarget> route(RoutingContext context) {
		Objects.requireNonNull(context);
		Objects.requireNonNull(context.getCertificateChain());
		if (context.getCertificateChain()[0].getSubjectDN().getName().contains(pattern)) {
			return Optional.of(target);
		} else {
			return Optional.empty();
		}
	}
}
