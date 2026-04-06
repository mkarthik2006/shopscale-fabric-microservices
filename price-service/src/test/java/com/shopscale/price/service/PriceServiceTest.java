package com.shopscale.price.service;

import com.shopscale.common.dto.PriceResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceServiceTest {

    private final PriceService priceService = new PriceService();

    @Test
    @DisplayName("getPrice - returns correct price for known SKU P1")
    void getPrice_shouldReturnPriceForP1() {
        PriceResponseDto result = priceService.getPrice("P1");
        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }

    @Test
    @DisplayName("getPrice - returns correct price for known SKU P2")
    void getPrice_shouldReturnPriceForP2() {
        PriceResponseDto result = priceService.getPrice("P2");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("89.50"));
    }

    @Test
    @DisplayName("getPrice - is case-insensitive")
    void getPrice_shouldBeCaseInsensitive() {
        PriceResponseDto result = priceService.getPrice("p3");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("14.99"));
    }

    @Test
    @DisplayName("getPrice - throws for unknown SKU")
    void getPrice_shouldThrowForUnknownSku() {
        assertThatThrownBy(() -> priceService.getPrice("UNKNOWN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pricing not found for SKU: UNKNOWN");
    }
}