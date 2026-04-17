package com.shopscale.cart.service;

import com.shopscale.common.dto.PriceResponseDto;
import com.shopscale.common.dto.StandardResponse;
import com.shopscale.common.exception.BusinessException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceClientService {

    private final RestTemplate restTemplate;

    @Value("${services.price-service.url:http://price-service}")
    private String priceServiceUrl;

    
    @Retry(name = "priceService", fallbackMethod = "fallbackPrice")
    @CircuitBreaker(name = "priceService", fallbackMethod = "fallbackPrice")
    public PriceResponseDto getPrice(String sku) {

        log.info("Calling Price Service for SKU: {}", sku);

        ParameterizedTypeReference<StandardResponse<PriceResponseDto>> responseType =
                new ParameterizedTypeReference<>() {};

        StandardResponse<PriceResponseDto> responseWrapper = restTemplate.exchange(
                priceServiceUrl + "/api/v1/prices/" + sku,
                HttpMethod.GET,
                null,
                responseType
        ).getBody();

        if (responseWrapper == null || responseWrapper.data() == null) {
            throw new BusinessException("Invalid or empty response from Price Service");
        }

        return responseWrapper.data();
    }

    /**
     * Graceful degradation fallback
     */
    public PriceResponseDto fallbackPrice(String sku, Throwable ex) {

        log.warn("🚨 Circuit Breaker triggered for SKU: {}", sku, ex);

        return new PriceResponseDto(
                sku,
                BigDecimal.ZERO,
                "USD",
                "FALLBACK"
        );
    }
}