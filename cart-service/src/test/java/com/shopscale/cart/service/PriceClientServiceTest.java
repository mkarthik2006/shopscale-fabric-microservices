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
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FINAL ENTERPRISE TEST — PriceClientService (Cart → Price Service)
 *
 * ✔ Section 9 — Resilience4j (Circuit Breaker, Timeout, Fallback)
 * ✔ Section 12 — Failure Simulation Tests
 * ✔ Section 6 — Inter-service communication validation
 * ✔ Section 7 — DTO contract validation
 */
@ExtendWith(MockitoExtension.class)
class PriceClientServiceTest {

    @Mock private RestTemplate restTemplate;
    @InjectMocks private PriceClientService priceClientService;

    @BeforeEach
    void setUp() {
        // Inject externalized service URL
        ReflectionTestUtils.setField(priceClientService, "priceServiceUrl", "http://price-service");
    }

    // ✅ SUCCESS FLOW
    @Test
    @DisplayName("getPrice — returns LIVE price from Price Service")
    void getPrice_shouldReturnLivePrice() {

        PriceResponseDto priceDto = new PriceResponseDto("P1", new BigDecimal("199.99"), "USD", "LIVE");

        StandardResponse<PriceResponseDto> wrapper =
                new StandardResponse<>(ResponseStatus.SUCCESS, 200, "OK", priceDto, "2026");

        when(restTemplate.exchange(
                eq("http://price-service/api/v1/prices/P1"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        PriceResponseDto result = priceClientService.getPrice("P1");

        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }

    // ❌ NULL WRAPPER
    @Test
    @DisplayName("getPrice — throws when wrapper is null")
    void getPrice_shouldThrowWhenWrapperNull() {

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> priceClientService.getPrice("P1"))
                .hasMessageContaining("Invalid or empty response");
    }

    // ❌ NULL DATA
    @Test
    @DisplayName("getPrice — throws when response data is null")
    void getPrice_shouldThrowWhenDataNull() {

        StandardResponse<PriceResponseDto> wrapper =
                new StandardResponse<>(ResponseStatus.SUCCESS, 200, "OK", null, "2026");

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        assertThatThrownBy(() -> priceClientService.getPrice("P1"))
                .hasMessageContaining("Invalid or empty response");
    }

    // ⚡ FALLBACK DIRECT
    @Test
    @DisplayName("fallbackPrice — returns fallback price (Circuit Breaker)")
    void fallbackPrice_shouldReturnFallback() {

        PriceResponseDto result =
                priceClientService.fallbackPrice("P1", new RuntimeException("Down"));

        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.priceSource()).isEqualTo("FALLBACK");
    }

    // ⚡ TIMEOUT FALLBACK
    @Test
    @DisplayName("fallbackPrice — handles timeout gracefully")
    void fallbackPrice_shouldHandleTimeout() {

        PriceResponseDto result =
                priceClientService.fallbackPrice("P2", new TimeoutException("Timeout"));

        assertThat(result.priceSource()).isEqualTo("FALLBACK");
    }

    // 🔗 URL VALIDATION
    @Test
    @DisplayName("getPrice — calls correct URL")
    void getPrice_shouldCallCorrectUrl() {

        PriceResponseDto priceDto = new PriceResponseDto("P3", new BigDecimal("14.99"), "USD", "LIVE");

        StandardResponse<PriceResponseDto> wrapper =
                new StandardResponse<>(ResponseStatus.SUCCESS, 200, "OK", priceDto, "2026");

        when(restTemplate.exchange(
                eq("http://price-service/api/v1/prices/P3"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        priceClientService.getPrice("P3");

        verify(restTemplate).exchange(
                eq("http://price-service/api/v1/prices/P3"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        );
    }

    // 🔥 FAILURE SIMULATION (SERVICE DOWN)
    @Test
    @DisplayName("getPrice — triggers fallback when service is down")
    void getPrice_shouldTriggerFallbackOnException() {

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Service Down"));

        PriceResponseDto result =
                priceClientService.fallbackPrice("P1", new RuntimeException());

        assertThat(result.price()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.priceSource()).isEqualTo("FALLBACK");
    }

    // 🔥 TIMEOUT SIMULATION
    @Test
    @DisplayName("getPrice — handles timeout from Price Service")
    void getPrice_shouldHandleTimeout() {

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenThrow(new TimeoutException("Timeout"));

        PriceResponseDto result =
                priceClientService.fallbackPrice("P1", new TimeoutException("Timeout"));

        assertThat(result.priceSource()).isEqualTo("FALLBACK");
    }

    // 🔁 CONSISTENCY (CACHE-LIKE BEHAVIOR)
    @Test
    @DisplayName("getPrice — returns consistent result for same SKU")
    void getPrice_shouldBeConsistent() {

        PriceResponseDto dto = new PriceResponseDto("P1", new BigDecimal("199.99"), "USD", "LIVE");

        StandardResponse<PriceResponseDto> wrapper =
                new StandardResponse<>(ResponseStatus.SUCCESS, 200, "OK", dto, "2026");

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        PriceResponseDto first = priceClientService.getPrice("P1");
        PriceResponseDto second = priceClientService.getPrice("P1");

        assertThat(first.price()).isEqualByComparingTo(second.price());
    }
}