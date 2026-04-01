package com.shopscale.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitKeyResolverConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitKeyResolverConfig.class);

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .filter(p -> p instanceof Authentication)
                .cast(Authentication.class)

                //HARDENING: Only count real users; ignore anonymous "fallback" tokens
                .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))

                .map(Authentication::getName)
                .doOnNext(user -> log.debug("Rate limiting for Authenticated User: {}", user))

                .switchIfEmpty(Mono.defer(() -> {
                    // Fallback to Remote Address (IP) for anonymous/guest sessions
                    InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

                    String ip = (remoteAddress != null && remoteAddress.getAddress() != null)
                            ? remoteAddress.getAddress().getHostAddress()
                            : "unknown";

                    log.debug("Rate limiting fallback for Anonymous/IP bucket: {}", ip);
                    return Mono.just(ip);
                }));
    }
}
