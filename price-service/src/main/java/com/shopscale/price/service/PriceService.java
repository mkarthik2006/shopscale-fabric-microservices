package com.shopscale.price.service;
import com.shopscale.common.dto.PriceResponseDto;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PriceService {

    // Mocking a database or external pricing engine for now
    private final Map<String, BigDecimal> MOCK_PRICES = Map.of(
            "P1", new BigDecimal("199.99"),
            "P2", new BigDecimal("89.50"),
            "P3", new BigDecimal("14.99")
    );

   
    @Cacheable(value = "prices", key = "#sku", unless = "#result == null")
    public PriceResponseDto getPrice(String sku) {
        
        // This log will automatically pick up Zipkin Trace/Span IDs!
        log.info("Cache miss! Fetching live price for SKU: {}", sku);

        
        BigDecimal price = Optional.ofNullable(MOCK_PRICES.get(sku.toUpperCase()))
                .orElseThrow(() -> new RuntimeException("Pricing not found for SKU: " + sku));

        return new PriceResponseDto(
                sku,
                price,
                "USD",
                "LIVE"
        );
    }
}
