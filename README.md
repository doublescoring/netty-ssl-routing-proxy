# netty-ssl-routing-proxy

Simple ssl frontend with ability to route client connections to various backend
depending on client certificate.

## Use case

The main use case is to serve multiple connection types of various protocols
with only one frontend SSL server (host:port). Most corporate firewalls
restricts SSL connections to 443 port only.

## Build

    mvn clean package docker:build -Pdocker

The command will build docker image _netty-ssl-routing-proxy_.
-Pdocker profile is required to switch platform specific JNI for netty to linux-x86_64.

## Configuration

See example [config.json](https://github.com/doublescoring/netty-ssl-routing-proxy/blob/master/src/test/resources/com/doublescoring/netty/proxy/config/config.json)

Routing conditions supported:
* Certificate subject substring matching (e.g. _OU=SERVICE-NAME_)
* Specific intermediate certificate in client certificate chain

## Usage

Deploying using docker-compose.yml:
```
proxy:
  image: netty-ssl-routing-proxy
  restart: always
  volumes:
    - /path-to-configs/config.json:/etc/netty-ssl-routing-proxy.conf:ro
    - /path-to-configs/server.jks:ro
    - /path-to-configs/truststore.jks:/etc/ssl/truststore.jks:ro
  ports:
    - "0.0.0.0:443:443"
```
