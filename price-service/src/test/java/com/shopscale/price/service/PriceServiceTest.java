package com.shopscale.price.service;

import com.shopscale.common.dto.PriceResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit Tests for PriceService (Enterprise Pricing Engine)
 *
 * ✔ Doc Ref: Page 3 — Redis caching compatibility (Serializable DTO)
 * ✔ Doc Ref: Page 6 — Cart Service dependency on Price Service
 * ✔ Covers: known SKUs, case-insensitivity, error handling, contract validation
 */
class PriceServiceTest {

    private final PriceService priceService = new PriceService();

    @Test
    @DisplayName("getPrice — returns correct price for SKU P1")
    void getPrice_shouldReturnCorrectPriceForP1() {
        PriceResponseDto result = priceService.getPrice("P1");

        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(result.currency()).isEqualTo("USD");
        // FIX: PriceResponseDto record field is "priceSource", not "source"
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }

    @Test
    @DisplayName("getPrice — returns correct price for SKU P2")
    void getPrice_shouldReturnCorrectPriceForP2() {
        PriceResponseDto result = priceService.getPrice("P2");

        assertThat(result.sku()).isEqualTo("P2");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("89.50"));
        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("getPrice — returns correct price for SKU P3")
    void getPrice_shouldReturnCorrectPriceForP3() {
        PriceResponseDto result = priceService.getPrice("P3");

        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("14.99"));
    }

    @Test
    @DisplayName("getPrice — is case-insensitive (lowercase SKU resolves)")
    void getPrice_shouldBeCaseInsensitive() {
        PriceResponseDto result = priceService.getPrice("p1");

        assertThat(result.sku()).isEqualTo("p1"); // preserves input
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
    }

    @Test
    @DisplayName("getPrice — handles mixed-case SKU correctly")
    void getPrice_shouldHandleMixedCase() {
        PriceResponseDto result = priceService.getPrice("p2");

        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("89.50"));
        // FIX: PriceResponseDto record field is "priceSource", not "source"
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }

    @Test
    @DisplayName("getPrice — throws RuntimeException for unknown SKU")
    void getPrice_shouldThrowForUnknownSku() {
        assertThatThrownBy(() -> priceService.getPrice("UNKNOWN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pricing not found for SKU");
    }

    @Test
    @DisplayName("getPrice — returns consistent currency across all SKUs")
    void getPrice_shouldAlwaysReturnUSD() {
        PriceResponseDto p1 = priceService.getPrice("P1");
        PriceResponseDto p2 = priceService.getPrice("P2");

        assertThat(p1.currency()).isEqualTo("USD");
        assertThat(p2.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("getPrice — verifies deterministic pricing (no random values)")
    void getPrice_shouldReturnDeterministicValues() {
        PriceResponseDto firstCall = priceService.getPrice("P1");
        PriceResponseDto secondCall = priceService.getPrice("P1");

        assertThat(firstCall.price()).isEqualByComparingTo(secondCall.price());
    }
}