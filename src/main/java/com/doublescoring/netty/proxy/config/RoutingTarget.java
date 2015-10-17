package com.doublescoring.netty.proxy.config;

import java.util.Objects;

/**
 * Routing target with host and port of the target server.
 */
public class RoutingTarget {
	private final String host;
	private final int port;

	public RoutingTarget(String host, int port) {
		this.host = Objects.requireNonNull(host);
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return host + ':' + port;
	}
}
