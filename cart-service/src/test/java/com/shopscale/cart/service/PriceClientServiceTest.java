package com.shopscale.cart.service;

import com.shopscale.common.dto.PriceResponseDto;
import com.shopscale.common.dto.StandardResponse;
import com.shopscale.common.dto.ResponseStatus;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceClientServiceTest {

    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private PriceClientService priceClientService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(priceClientService, "priceServiceUrl", "http://price-service");
    }

    @Test
    @DisplayName("getPrice - returns live price from Price Service")
    void getPrice_shouldReturnLivePrice() {
        PriceResponseDto priceDto = new PriceResponseDto("P1", new BigDecimal("199.99"), "USD", "LIVE");
        StandardResponse<PriceResponseDto> wrapper = new StandardResponse<>(
                ResponseStatus.SUCCESS, 200, "Request successful", priceDto, "2026-01-01T00:00:00Z"
        );

        when(restTemplate.exchange(
                eq("http://price-service/api/v1/prices/P1"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(wrapper));

        PriceResponseDto result = priceClientService.getPrice("P1");

        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(result.priceSource()).isEqualTo("LIVE");
    }

    @Test
    @DisplayName("fallbackPrice - returns FALLBACK price with zero value")
    void fallbackPrice_shouldReturnFallbackResponse() {
        PriceResponseDto result = priceClientService.fallbackPrice("P1", new RuntimeException("Connection refused"));

        assertThat(result.sku()).isEqualTo("P1");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.priceSource()).isEqualTo("FALLBACK");
        assertThat(result.currency()).isEqualTo("USD");
    }
}