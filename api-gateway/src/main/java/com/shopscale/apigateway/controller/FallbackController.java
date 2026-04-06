package com.shopscale.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Enterprise Fallback Controller (Week 3 Mandate)
 * Handles circuit-breaker fallback URIs configured in gateway routes.
 * Without this, fallbackUri: forward:/fallback/* returns 404.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/products")
    public Mono<ResponseEntity<Map<String, Object>>> productsFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Product Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> ordersFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Order Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/inventory")
    public Mono<ResponseEntity<Map<String, Object>>> inventoryFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Inventory Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/cart")
    public Mono<ResponseEntity<Map<String, Object>>> cartFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildFallback("Cart Service is temporarily unavailable. Please try again later.")));
    }

    @GetMapping("/prices")
    public Mono<ResponseEntity<Map<String, Object>>> pricesFallback() {
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