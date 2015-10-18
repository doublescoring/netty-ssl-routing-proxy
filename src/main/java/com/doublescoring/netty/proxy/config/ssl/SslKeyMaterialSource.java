package com.doublescoring.netty.proxy.config.ssl;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Interface for ssl key material.
 */
public interface SslKeyMaterialSource {
	X509Certificate[] getCertificateChain();

	PrivateKey getPrivateKey();
}
