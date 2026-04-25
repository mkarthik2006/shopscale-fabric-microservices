package com.shopscale.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;

/**
 * PROJECT_RULES.md Week 3:
 * 100 HTTP requests per minute per client IP (Redis-backed).
 * Enhanced with distributed tracing logs (traceId, spanId).
 */
@Component
public class DistributedMinuteRateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DistributedMinuteRateLimitGlobalFilter.class);

    private final ReactiveStringRedisTemplate redis;

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final String KEY_PREFIX = "gw:ratelimit:minute:v1:";
    private static final Duration KEY_TTL = Duration.ofSeconds(150);

    public DistributedMinuteRateLimitGlobalFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getPath().value();


        if (path.startsWith("/actuator/") || path.startsWith("/auth/") || path.startsWith("/fallback/")) {
            return chain.filter(exchange);
        }


        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        long minuteBucket = Instant.now().getEpochSecond() / 60;
        String clientKey = "ip:" + clientIp(exchange);
        String redisKey = KEY_PREFIX + minuteBucket + ":" + clientKey;


        log.info("Incoming request | path={} ip={} traceId={} spanId={}",
                path,
                clientKey,
                MDC.get("traceId"),
                MDC.get("spanId")
        );

        return redis.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count != null && count == 1L) {
                        return redis.expire(redisKey, KEY_TTL).thenReturn(count);
                    }
                    return Mono.justOrEmpty(count);
                })
                .flatMap(count -> {
                    if (count != null && count > MAX_REQUESTS_PER_MINUTE) {


                        log.warn("Rate limit exceeded | path={} ip={} count={} traceId={} spanId={}",
                                path,
                                clientKey,
                                count,
                                MDC.get("traceId"),
                                MDC.get("spanId")
                        );

                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }

                    return chain.filter(exchange);
                });
    }

    private static String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        return 5;
    }
}