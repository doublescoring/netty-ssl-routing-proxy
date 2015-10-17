package com.doublescoring.netty.proxy.config;

import javax.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Routing context for RoutingRule.
 */
public class RoutingContext {
	X509Certificate[] certificateChain;

	public X509Certificate[] getCertificateChain() {
		return certificateChain;
	}

	public void setCertificateChain(X509Certificate[] certificateChain) {
		this.certificateChain = certificateChain;
	}

	@Override
	public String toString() {
		return "RoutingContext{" +
				"certificateChain=" + Arrays.toString(certificateChain) +
				'}';
	}
}
