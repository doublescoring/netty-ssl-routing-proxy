package com.doublescoring.netty.proxy;

import com.doublescoring.netty.proxy.config.NettySslRoutingProxyConfig;
import com.doublescoring.netty.proxy.config.RoutingRule;
import com.doublescoring.netty.proxy.config.ssl.SslKeyMaterialSource;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Config builder for tests. It always generates random port.
 */
public class TestNettySslRoutingProxyConfig implements NettySslRoutingProxyConfig {
	private int port;
	private RoutingRule rule;
	private List<X509Certificate> trusted = new ArrayList<>();
	private SslKeyMaterialSource sslKeyMaterialSource;

	private TestNettySslRoutingProxyConfig(){
	}

	public TestNettySslRoutingProxyConfig withTrustedCertificate(X509Certificate certificate) {
		this.trusted.add(certificate);
		return this;
	}

	public TestNettySslRoutingProxyConfig withSslKeyMaterialSource(SslKeyMaterialSource source) {
		this.sslKeyMaterialSource = source;
		return this;
	}

	public TestNettySslRoutingProxyConfig withRoutingRule(RoutingRule rule) {
		this.rule = rule;
		return this;
	}

	public int getBindPort() {
		return port;
	}

	public String getBindHost() {
		return "localhost";
	}

	@Override
	public List<X509Certificate> getTrustedCertificates() {
		return trusted;
	}

	@Override
	public SslKeyMaterialSource getKeyMaterialSource() {
		return sslKeyMaterialSource;
	}

	@Override
	public RoutingRule getRoutingRule() {
		return rule;
	}

	public static TestNettySslRoutingProxyConfig create() throws IOException {
		TestNettySslRoutingProxyConfig config = new TestNettySslRoutingProxyConfig();
		config.port = new ServerSocket(0).getLocalPort();
		return config;
	}
}
