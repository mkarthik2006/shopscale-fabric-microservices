package com.shopscale.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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

                // ✅ Only real authenticated users
                .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))

                .map(Authentication::getName)
                .doOnNext(user -> log.debug(
                        "Rate limit key (USER) = {} traceId={} spanId={}",
                        user,
                        MDC.get("traceId"),
                        MDC.get("spanId")
                ))

                .switchIfEmpty(Mono.defer(() -> {

                    // ✅ Check X-Forwarded-For first (important for Docker/Nginx)
                    String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

                    String ip;
                    if (forwarded != null && !forwarded.isBlank()) {
                        ip = forwarded.split(",")[0].trim();
                    } else {
                        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
                        ip = (remote != null && remote.getAddress() != null)
                                ? remote.getAddress().getHostAddress()
                                : "unknown";
                    }

                    log.debug(
                            "Rate limit key (IP) = {} traceId={} spanId={}",
                            ip,
                            MDC.get("traceId"),
                            MDC.get("spanId")
                    );

                    return Mono.just(ip);
                }));
    }
}