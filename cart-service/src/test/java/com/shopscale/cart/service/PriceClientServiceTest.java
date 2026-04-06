package com.shopscale.cart.service;

import com.shopscale.common.dto.PriceResponseDto;
import com.shopscale.common.dto.ResponseStatus;
import com.shopscale.common.dto.StandardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for PriceClientService (Cart Service → Price Service)
 * Doc Ref: Page 6 — "Mandatory implementation of Circuit Breakers in at least one inter-service communication path
 *                     (e.g., the Cart Service calling the Price Service)"
 * Doc Ref: Page 6 — "ensure the system fails fast instead of blocking threads when a dependency is unavailable"
 */
@ExtendWith(MockitoExtension.class)
class PriceClientServiceTest {

    @Mock private RestTemplate restTemplate;
    @InjectMocks private PriceClientService priceClientService;

    @BeforeEach
    void setUp() {
        // PriceClientService.java L28: @Value("${services.price-service.url:http://price-service}")
        ReflectionTestUtils.setField(priceClientService, "priceServiceUrl", "http://price-service");
    }

    @Test
    @DisplayName("getPrice — returns LIVE price from Price Service via RestTemplate")
    void getPrice_shouldReturnLivePrice() {
        // Given: Price Service returns a valid wrapped response
        PriceResponseDto priceDto = new PriceResponseDto("P1", new BigDecimal("199.99"), "USD", "LIVE");
        StandardResponse<PriceResponseDto> wrapper = new StandardResponse<>(
                ResponseStatus.SUCCESS, 200, "Request successful", priceDto, "2026-04-01T00:00:00Z"
        );

        when(restTemplate.exchange(
                eq("http://price-service/api/v1/prices/P1"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        // When
        PriceResponseDto result = priceClientService.getPrice("P1");

        // Then
        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }

    @Test
    @DisplayName("getPrice — throws BusinessException when response wrapper is null")
    void getPrice_shouldThrowWhenResponseNull() {
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(null));

        // PriceClientService.java L48: "Invalid or empty response from Price Service"
        assertThatThrownBy(() -> priceClientService.getPrice("P1"))
                .hasMessageContaining("Invalid or empty response from Price Service");
    }

    @Test
    @DisplayName("getPrice — throws BusinessException when data inside wrapper is null")
    void getPrice_shouldThrowWhenDataNull() {
        StandardResponse<PriceResponseDto> wrapper = new StandardResponse<>(
                ResponseStatus.SUCCESS, 200, "Request successful", null, "2026-04-01T00:00:00Z"
        );

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        assertThatThrownBy(() -> priceClientService.getPrice("P1"))
                .hasMessageContaining("Invalid or empty response from Price Service");
    }

    @Test
    @DisplayName("fallbackPrice — returns FALLBACK price with BigDecimal.ZERO (Circuit Breaker)")
    void fallbackPrice_shouldReturnFallbackResponse() {
        // This is the @CircuitBreaker fallbackMethod — called when Price Service is DOWN
        PriceResponseDto result = priceClientService.fallbackPrice("P1", new RuntimeException("Connection refused"));

        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.priceSource()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("fallbackPrice — handles different exception types gracefully")
    void fallbackPrice_shouldHandleTimeoutException() {
        PriceResponseDto result = priceClientService.fallbackPrice("P2",
                new java.util.concurrent.TimeoutException("Read timed out"));

        assertThat(result.sku()).isEqualTo("P2");
        assertThat(result.priceSource()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("getPrice — calls correct URL with SKU path variable")
    void getPrice_shouldCallCorrectUrl() {
        PriceResponseDto priceDto = new PriceResponseDto("P3", new BigDecimal("14.99"), "USD", "LIVE");
        StandardResponse<PriceResponseDto> wrapper = new StandardResponse<>(
                ResponseStatus.SUCCESS, 200, "OK", priceDto, "2026-04-01T00:00:00Z"
        );

        when(restTemplate.exchange(
                eq("http://price-service/api/v1/prices/P3"),
                eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        priceClientService.getPrice("P3");

        // Verify the exact URL format: {priceServiceUrl}/api/v1/prices/{sku}
        verify(restTemplate).exchange(
                eq("http://price-service/api/v1/prices/P3"),
                eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        );
    }
}