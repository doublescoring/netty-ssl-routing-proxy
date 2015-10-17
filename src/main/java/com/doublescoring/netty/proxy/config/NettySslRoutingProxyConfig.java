package com.doublescoring.netty.proxy.config;

import com.doublescoring.netty.proxy.config.ssl.SslContextConfiguration;

/**
 * Interface for server configuration.
 */
public interface NettySslRoutingProxyConfig {
	SslContextConfiguration getSslContextConfiguration();

	int getBindPort();

	String getBindHost();

	RoutingRule getRoutingRule();
}
