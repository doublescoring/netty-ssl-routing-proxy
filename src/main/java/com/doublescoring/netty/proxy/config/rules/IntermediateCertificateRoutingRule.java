package com.doublescoring.netty.proxy.config.rules;

import com.doublescoring.netty.proxy.config.RoutingContext;
import com.doublescoring.netty.proxy.config.RoutingRule;
import com.doublescoring.netty.proxy.config.RoutingTarget;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

/**
 * Routes to the specified target if certificate chain contains intermediate CA with specified subject.
 */
public class IntermediateCertificateRoutingRule implements RoutingRule {
	private final RoutingTarget target;
	private final String caSubject;

	@JsonCreator
	public IntermediateCertificateRoutingRule(@JsonProperty("target") RoutingTarget target,
											  @JsonProperty("caSubject") String caSubject) {
		this.target = Objects.requireNonNull(target);
		this.caSubject = Objects.requireNonNull(caSubject);
	}

	@Override
	public Optional<RoutingTarget> route(RoutingContext context) {
		Objects.requireNonNull(context);
		Objects.requireNonNull(context.getCertificateChain());
		for (X509Certificate certificate : context.getCertificateChain()) {
			if (caSubject.equals(certificate.getIssuerDN().getName())) {
				return Optional.of(target);
			}
		}
		return Optional.empty();
	}

	public String getCaSubject() {
		return caSubject;
	}

	public RoutingTarget getTarget() {
		return target;
	}
}
