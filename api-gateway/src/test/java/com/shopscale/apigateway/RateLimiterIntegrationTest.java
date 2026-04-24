package com.shopscale.apigateway;

import com.shopscale.apigateway.config.ClientIpResolver;
import com.shopscale.apigateway.filter.DistributedMinuteRateLimitGlobalFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Enterprise Hardening Proof - Rate Limiting Breach Scenario
 * Verifies that the gateway returns 429 Too Many Requests after the 100 req/min threshold.
 * Mandate Ref: Week 3 — Sophisticated Rate Limiting
 */
public class RateLimiterIntegrationTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private DistributedMinuteRateLimitGlobalFilter filter;
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOperations;
    private GatewayFilterChain chain;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        valueOperations = Mockito.mock(ReactiveValueOperations.class);
        chain = Mockito.mock(GatewayFilterChain.class);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(chain.filter(any())).thenReturn(Mono.empty());

        ClientIpResolver resolver = new ClientIpResolver("");
        filter = new DistributedMinuteRateLimitGlobalFilter(redisTemplate, resolver);
    }

    @Test
    @DisplayName("Should block request with HTTP 429 when threshold (100) is exceeded")
    void shouldReturn429WhenLimitExceeded() {
        
        // 1. Simulate the 101st request (count = 101)
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(101L));

        // 2. Mock Exchange (Targeting /api/ endpoint)
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress("192.168.1.1", 12345))
                        .build());

        // 3. Execute Filter
        Mono<Void> result = filter.filter(exchange, chain);

        // 4. ✅ PROOF: Assert 429 response
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Should allow request when below threshold")
    void shouldAllowWhenBelowLimit() {
        
        // 1. Simulate the 5th request (count = 5)
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(5L));

        // 2. Mock Exchange
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products")
                        .remoteAddress(new InetSocketAddress("192.168.1.1", 12345))
                        .build());

        // 3. Execute Filter
        Mono<Void> result = filter.filter(exchange, chain);

        // 4. ✅ PROOF: Assert status is NOT 429 (remains default 200 or as set by chain)
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
