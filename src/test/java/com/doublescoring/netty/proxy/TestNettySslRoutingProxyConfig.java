package com.doublescoring.netty.proxy;

import com.doublescoring.netty.proxy.config.NettySslRoutingProxyConfig;
import com.doublescoring.netty.proxy.config.RoutingRule;
import com.doublescoring.netty.proxy.config.ssl.SslContextConfiguration;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Config builder for tests. It always generates random port.
 */
public class TestNettySslRoutingProxyConfig implements NettySslRoutingProxyConfig {
	private int port;
	private SslContextConfiguration sslContextConfiguration;
	private RoutingRule rule;

	private TestNettySslRoutingProxyConfig(){
	}

	public TestNettySslRoutingProxyConfig withSslContextConfiguration(SslContextConfiguration sslContextConfiguration) throws SSLException {
		this.sslContextConfiguration = sslContextConfiguration;
		return this;
	}

	public TestNettySslRoutingProxyConfig withRoutingRule(RoutingRule rule) {
		this.rule = rule;
		return this;
	}

	@Override
	public SslContextConfiguration getSslContextConfiguration() {
		return sslContextConfiguration;
	}

	public int getBindPort() {
		return port;
	}

	public String getBindHost() {
		return "localhost";
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
