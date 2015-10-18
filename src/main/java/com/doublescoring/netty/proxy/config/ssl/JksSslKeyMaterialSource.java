package com.doublescoring.netty.proxy.config.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * JKS implementations of SslKeyMaterialSource.
 */
public class JksSslKeyMaterialSource implements SslKeyMaterialSource {
	private PrivateKey privateKey;
	private X509Certificate[] certChain;

	public JksSslKeyMaterialSource(File keyStore, String password, String alias) throws Exception {
		try (FileInputStream stream = new FileInputStream(keyStore)) {
			KeyStore jks = KeyStore.getInstance("JKS");
			jks.load(stream, password.toCharArray());
			privateKey = (PrivateKey) jks.getKey(alias, password.toCharArray());
			Certificate[] chain = jks.getCertificateChain(alias);
			certChain = new X509Certificate[chain.length];
			for (int i = 0; i < chain.length; i++) {
				certChain[i] = (X509Certificate) chain[i];
			}
		}
	}
	@Override
	public X509Certificate[] getCertificateChain() {
		return certChain;
	}

	@Override
	public PrivateKey getPrivateKey() {
		return privateKey;
	}
}
