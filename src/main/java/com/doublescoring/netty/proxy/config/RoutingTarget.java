package com.doublescoring.netty.proxy.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Routing target with host and port of the target server.
 */
public class RoutingTarget {
	private final String host;
	private final int port;

	@JsonCreator
	public RoutingTarget(@JsonProperty("host") String host, @JsonProperty("port") int port) {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RoutingTarget target = (RoutingTarget) o;
		return Objects.equals(port, target.port) &&
				Objects.equals(host, target.host);
	}

	@Override
	public int hashCode() {
		return Objects.hash(host, port);
	}
}
