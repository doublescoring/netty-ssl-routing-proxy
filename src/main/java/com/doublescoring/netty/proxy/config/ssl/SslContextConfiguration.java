package com.doublescoring.netty.proxy.config.ssl;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple interface for SSL context configuration.
 */
public class SslContextConfiguration {
	private SslKeyMaterialSource source;
	private boolean clientAuth;
	private List<X509Certificate> trustedCA = new ArrayList<>();

	public void setSource(SslKeyMaterialSource source) {
		this.source = source;
	}

	public void setClientAuth(boolean clientAuth) {
		this.clientAuth = clientAuth;
	}

	public SslContextConfiguration withSource(SslKeyMaterialSource source) {
		setSource(source);
		return this;
	}

	public SslContextConfiguration withClientAuth(boolean clientAuth) {
		setClientAuth(clientAuth);
		return this;
	}

	public SslContextConfiguration withTrustedCertificate(X509Certificate cert) {
		this.trustedCA.add(cert);
		return this;
	}

	public SslContext getSslContext() throws SSLException {
		return SslContextBuilder.forServer(source.getPrivateKey(), source.getPassword(), source.getCertificateChain())
				.sslProvider(SslProvider.JDK)
				.trustManager(trustedCA.toArray(new X509Certificate[]{}))
				.clientAuth(clientAuth ? ClientAuth.REQUIRE : ClientAuth.NONE)
				.build();
	}
}
