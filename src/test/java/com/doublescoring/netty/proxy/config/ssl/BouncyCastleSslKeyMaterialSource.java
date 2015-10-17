package com.doublescoring.netty.proxy.config.ssl;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

/**
 * Generates self signed and signed by authority certificates using BC.
 */
public class BouncyCastleSslKeyMaterialSource implements SslKeyMaterialSource {

	private static final int KEY_SIZE = 1024;
	private static final Provider PROVIDER = new BouncyCastleProvider();

	private final PrivateKey key;
	private final X509Certificate[] chain;

	/**
	 * Generates self-signed certificate with CN=example.com valid for one month before and after now.
	 */
	public BouncyCastleSslKeyMaterialSource() throws Exception {
		this("example.com", null, null);
	}

	/**
	 * Generates self-sifned certificate with custom CN
	 */
	public BouncyCastleSslKeyMaterialSource(String fqdn) throws Exception {
		this(fqdn, null, null);
	}

	/**
	 * Generates certificate with custom CN signed with authority
	 */
	public BouncyCastleSslKeyMaterialSource(String fqdn, SslKeyMaterialSource caSource) throws Exception {
		this(fqdn, caSource.getPrivateKey(), caSource.getCertificateChain());
	}

	/**
	 * Generates certificate with CN=fqdn.
	 * @param signingKey Private key to sign certificate with. Generated certificate will be self-signed if
	 *            key == null
	 * @param caChain Optional CA chain to add to this certificate.
	 */
	public BouncyCastleSslKeyMaterialSource(String fqdn, PrivateKey signingKey, X509Certificate[] caChain)
			throws Exception {
		Date notBefore = Date.from(LocalDate.now().minusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC));
		Date notAfter = Date.from(LocalDate.now().plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC));
		final KeyPair keypair;
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(KEY_SIZE);
			keypair = keyGen.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		}
		key = keypair.getPrivate();

		X500Name owner = new X500Name("CN=" + fqdn);
		X500Name issuer = owner;
		if (caChain != null && caChain.length > 0) {
			issuer = new X500Name(caChain[0].getSubjectDN().getName());
		}

		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				issuer, new BigInteger(64, new Random()), notBefore, notAfter, owner, keypair.getPublic());

		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
				.build(Optional.ofNullable(signingKey).orElse(key));
		X509CertificateHolder certHolder = builder.build(signer);
		X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certHolder);

		if (caChain == null) {
			chain = new X509Certificate[]{cert};
		} else {
			chain = new X509Certificate[caChain.length + 1];
			chain[0] = cert;
			System.arraycopy(caChain, 0, chain, 1, caChain.length);
		}
	}

	@Override
	public X509Certificate[] getCertificateChain() {
		return chain;
	}

	@Override
	public PrivateKey getPrivateKey() {
		return key;
	}

	@Override
	public String getPassword() {
		return "";
	}
}
