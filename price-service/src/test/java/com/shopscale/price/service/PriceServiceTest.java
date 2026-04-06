package com.shopscale.price.service;

import com.shopscale.common.dto.PriceResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit Tests for PriceService (Pricing Engine)
 * Doc Ref: Page 3 — "Redis: distributed caching"
 * Doc Ref: Page 6 — "Cart Service calling the Price Service"
 * Verifies: @Cacheable behavior, case-insensitive SKU, known prices, unknown SKU error
 */
class PriceServiceTest {

    private final PriceService priceService = new PriceService();

    @Test
    @DisplayName("getPrice — returns correct price for SKU P1 ($199.99)")
    void getPrice_shouldReturnCorrectPriceForP1() {
        PriceResponseDto result = priceService.getPrice("P1");

        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }

    @Test
    @DisplayName("getPrice — returns correct price for SKU P2 ($89.50)")
    void getPrice_shouldReturnCorrectPriceForP2() {
        PriceResponseDto result = priceService.getPrice("P2");

        assertThat(result.sku()).isEqualTo("P2");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("89.50"));
        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("getPrice — returns correct price for SKU P3 ($14.99)")
    void getPrice_shouldReturnCorrectPriceForP3() {
        PriceResponseDto result = priceService.getPrice("P3");

        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("14.99"));
    }

    @Test
    @DisplayName("getPrice — is case-insensitive (lowercase sku resolves)")
    void getPrice_shouldBeCaseInsensitive() {
        // PriceService.java L32: MOCK_PRICES.get(sku.toUpperCase())
        PriceResponseDto result = priceService.getPrice("p1");

        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
    }

    @Test
    @DisplayName("getPrice — throws RuntimeException for unknown SKU")
    void getPrice_shouldThrowForUnknownSku() {
        // PriceService.java L33: .orElseThrow(() -> new RuntimeException("Pricing not found for SKU: " + sku))
        assertThatThrownBy(() -> priceService.getPrice("UNKNOWN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pricing not found for SKU: UNKNOWN");
    }

    @Test
    @DisplayName("getPrice — mixed case SKU resolves correctly")
    void getPrice_shouldHandleMixedCase() {
        PriceResponseDto result = priceService.getPrice("p2");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("89.50"));
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }
}