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

@Configuration
public class RateLimitKeyResolverConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitKeyResolverConfig.class);
    private final ClientIpResolver clientIpResolver;

    public RateLimitKeyResolverConfig(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .filter(p -> p instanceof Authentication)
                .cast(Authentication.class)


                .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))

                .map(Authentication::getName)
                .doOnNext(user -> log.debug(
                        "Rate limit key (USER) = {} traceId={} spanId={}",
                        user,
                        MDC.get("traceId"),
                        MDC.get("spanId")
                ))

                .switchIfEmpty(Mono.defer(() -> {
                    String ip = clientIpResolver.resolveClientIp(exchange.getRequest());

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