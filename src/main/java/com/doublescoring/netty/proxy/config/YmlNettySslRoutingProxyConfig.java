package com.doublescoring.netty.proxy.config;

import com.doublescoring.netty.proxy.config.ssl.SslContextConfiguration;
import io.netty.handler.ssl.SslContext;

/**
 * Yml file based configuration for the server
 * TODO
 */
public class YmlNettySslRoutingProxyConfig implements NettySslRoutingProxyConfig {
	public YmlNettySslRoutingProxyConfig(String file) {

	}

	public SslContext getSslContext() {
		return null;
	}

	@Override
	public SslContextConfiguration getSslContextConfiguration() {
		return null;
	}

	public int getBindPort() {
		return 0;
	}

	public String getBindHost() {
		return null;
	}

	@Override
	public RoutingRule getRoutingRule() {
		return null;
	}

}
