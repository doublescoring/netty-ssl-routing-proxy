package com.doublescoring.netty.proxy.config;

import com.doublescoring.netty.proxy.config.ssl.SslKeyMaterialSource;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Interface for server configuration.
 */
public interface NettySslRoutingProxyConfig {
	int getBindPort();

	String getBindHost();

	List<X509Certificate> getTrustedCertificates();

	SslKeyMaterialSource getKeyMaterialSource();

	RoutingRule getRoutingRule();
}
