package com.shopscale.price.controller;

import com.shopscale.common.dto.ResponseStatus;
import com.shopscale.price.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceControllerTest {

    @Mock
    private PriceService priceService;

    private PriceController controller;

    @BeforeEach
    void setUp() {
        controller = new PriceController(priceService);
    }

    @Test
    void getDefaultPrices_shouldReturnThreeSkusWrappedInStandardResponse() {
        when(priceService.getPrice("P1")).thenReturn(new com.shopscale.common.dto.PriceResponseDto("P1", new BigDecimal("10.00"), "USD", "LIVE"));
        when(priceService.getPrice("P2")).thenReturn(new com.shopscale.common.dto.PriceResponseDto("P2", new BigDecimal("20.00"), "USD", "LIVE"));
        when(priceService.getPrice("P3")).thenReturn(new com.shopscale.common.dto.PriceResponseDto("P3", new BigDecimal("30.00"), "USD", "LIVE"));

        var response = controller.getDefaultPrices();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getBody().data()).hasSize(3);
        verify(priceService).getPrice("P1");
        verify(priceService).getPrice("P2");
        verify(priceService).getPrice("P3");
    }

    @Test
    void getPrice_shouldDelegateAndWrapResponse() {
        var dto = new com.shopscale.common.dto.PriceResponseDto("SKU-1", new BigDecimal("99.99"), "USD", "CACHE");
        when(priceService.getPrice("SKU-1")).thenReturn(dto);

        var response = controller.getPrice("SKU-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getBody().data()).isEqualTo(dto);
        verify(priceService).getPrice("SKU-1");
    }
}
