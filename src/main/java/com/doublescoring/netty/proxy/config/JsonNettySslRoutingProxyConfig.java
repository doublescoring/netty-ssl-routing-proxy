package com.doublescoring.netty.proxy.config;

import com.doublescoring.netty.proxy.config.ssl.JksSslKeyMaterialSource;
import com.doublescoring.netty.proxy.config.ssl.SslKeyMaterialSource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Json configuration for the server.
 */
public class JsonNettySslRoutingProxyConfig implements NettySslRoutingProxyConfig {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private String keyStore;

	private String keyAlias;

	private String password;

	private String trustStore;

	private SslKeyMaterialSource keyMaterialSource;

	private List<X509Certificate> trustedCertificates;

	private int port;

	private String host;

	@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property = "@name")
	private RoutingRule routingRule;

	@Override
	public int getBindPort() {
		return port;
	}

	@Override
	public String getBindHost() {
		return host;
	}

	@Override
	public List<X509Certificate> getTrustedCertificates() {
		return trustedCertificates;
	}

	@Override
	public SslKeyMaterialSource getKeyMaterialSource() {
		return keyMaterialSource;
	}

	@Override
	public RoutingRule getRoutingRule() {
		return routingRule;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setRoutingRule(RoutingRule routingRule) {
		this.routingRule = routingRule;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}

	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}

	private JsonNettySslRoutingProxyConfig() {
	}

	public static JsonNettySslRoutingProxyConfig parse(String path) throws Exception {
		return parse(path, null);
	}

	public static JsonNettySslRoutingProxyConfig parse(String path, File basePath) throws Exception {
		JsonNettySslRoutingProxyConfig config = MAPPER.readValue(new File(path), JsonNettySslRoutingProxyConfig.class);

		File keyStoreFile = basePath == null ? new File(config.keyStore) : new File(basePath, config.keyStore);
		File trustStoreFile = basePath == null ? new File(config.trustStore) : new File(basePath, config.trustStore);

		config.keyMaterialSource = new JksSslKeyMaterialSource(keyStoreFile, config.password, config.keyAlias);

		ArrayList<X509Certificate> trusted = new ArrayList<>();
		KeyStore jks = KeyStore.getInstance("JKS");
		try (FileInputStream stream = new FileInputStream(trustStoreFile)) {
			jks.load(stream, null);
			Enumeration<String> aliases = jks.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				trusted.add((X509Certificate) jks.getCertificate(alias));
			}
		}
		config.trustedCertificates = Collections.unmodifiableList(trusted);

		return config;
	}

}
