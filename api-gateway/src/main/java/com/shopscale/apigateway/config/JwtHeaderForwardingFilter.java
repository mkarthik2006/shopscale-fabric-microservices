package com.shopscale.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtHeaderForwardingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Header mutation on reactive request is currently unstable in this stack and
        // caused UnsupportedOperationException for authenticated routes. Keep this
        // filter as a safe pass-through until explicit header propagation is reworked.
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() { return -1; }
}
