package com.shopscale.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Enterprise Fallback Controller (CircuitBreaker Compliance)
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/products")
    public Mono<ResponseEntity<Map<String, Object>>> productsFallback() {
        log.warn("Fallback triggered: PRODUCT-SERVICE traceId={} spanId={}",
                MDC.get("traceId"), MDC.get("spanId"));

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Product Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> ordersFallback() {
        log.warn("Fallback triggered: ORDER-SERVICE traceId={} spanId={}",
                MDC.get("traceId"), MDC.get("spanId"));

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Order Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/inventory")
    public Mono<ResponseEntity<Map<String, Object>>> inventoryFallback() {
        log.warn("Fallback triggered: INVENTORY-SERVICE traceId={} spanId={}",
                MDC.get("traceId"), MDC.get("spanId"));

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Inventory Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/cart")
    public Mono<ResponseEntity<Map<String, Object>>> cartFallback() {
        log.warn("Fallback triggered: CART-SERVICE traceId={} spanId={}",
                MDC.get("traceId"), MDC.get("spanId"));

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Cart Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/prices")
    public Mono<ResponseEntity<Map<String, Object>>> pricesFallback() {
        log.warn("Fallback triggered: PRICE-SERVICE traceId={} spanId={}",
                MDC.get("traceId"), MDC.get("spanId"));

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Price Service is temporarily unavailable. Please try again later.")));
    }

    private Map<String, Object> buildFallback(String message) {
        return Map.of(
                "status", "FAILURE",
                "code", 503,
                "message", message,
                "data", Map.of(),
                "timestamp", Instant.now().toString()
        );
    }
}