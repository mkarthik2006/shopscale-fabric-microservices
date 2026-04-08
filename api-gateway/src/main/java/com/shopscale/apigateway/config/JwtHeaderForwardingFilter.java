package com.shopscale.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.stream.Collectors;

@Component
public class JwtHeaderForwardingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .filter(Authentication::isAuthenticated)
            .cast(JwtAuthenticationToken.class)
            .flatMap(jwtAuth -> {
                String userId = jwtAuth.getToken().getSubject();
                String roles = jwtAuth.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.joining(","));

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(builder -> builder
                                .header("X-User-Id", userId)
                                .header("X-Roles", roles)
                        ).build();

                return chain.filter(mutatedExchange);
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() { return -1; }
}
