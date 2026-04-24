package com.shopscale.price.service;

import com.shopscale.common.dto.PriceResponseDto;
import com.shopscale.common.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PriceService {

    private final Map<String, BigDecimal> MOCK_PRICES = Map.of(
            "P1", new BigDecimal("199.99"),
            "P2", new BigDecimal("89.50"),
            "P3", new BigDecimal("14.99")
    );

    @Cacheable(value = "prices", key = "#sku", unless = "#result == null")
    public PriceResponseDto getPrice(String sku) {

        log.info("Fetching price | sku={} traceId={} spanId={}",
                sku,
                MDC.get("traceId"),
                MDC.get("spanId")
        );

        BigDecimal price = Optional.ofNullable(MOCK_PRICES.get(sku.toUpperCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Pricing not found for SKU: " + sku));

        return new PriceResponseDto(
                sku,
                price,
                "USD",
                "LIVE"
        );
    }
}