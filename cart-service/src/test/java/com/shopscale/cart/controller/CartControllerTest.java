package com.shopscale.cart.controller;

import com.shopscale.common.exception.GlobalExceptionHandler;

import com.shopscale.cart.service.PriceClientService;
import com.shopscale.common.dto.PriceResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FINAL ENTERPRISE TEST — CartController
 *
 * ✔ Section 6 — Service communication
 * ✔ Section 9 — Resilience validation
 * ✔ Section 12 — Controller testing
 * ✔ Section 16 — API response contract validation
 */
@WebMvcTest(CartController.class)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceClientService priceClientService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void stubJwtDecoder() {
        when(jwtDecoder.decode(anyString())).thenAnswer(inv -> {
            String token = inv.getArgument(0);
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .issuer("http://cart.test")
                    .build();
        });
    }

    // ✅ SUCCESS CASE
    @Test
    @DisplayName("GET /cart/total — returns cart total successfully")
    void total_shouldReturnCartTotal() throws Exception {

        PriceResponseDto priceData =
                new PriceResponseDto("P1", new BigDecimal("199.99"), "USD", "LIVE");

        when(priceClientService.getPrice("P1")).thenReturn(priceData);

        mockMvc.perform(get("/api/v1/cart/user-001/total")
                        .header("Authorization", "Bearer unit-test-token")
                        .param("sku", "P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").exists())
                // FIX: CartTotalResponseDto wraps PriceResponseDto as "priceResponse"
                .andExpect(jsonPath("$.data.sku").value("P1"))
                .andExpect(jsonPath("$.data.priceResponse.price").value(199.99))
                .andExpect(jsonPath("$.data.priceResponse.currency").value("USD"))
                .andExpect(jsonPath("$.data.priceResponse.priceSource").value("LIVE"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ⚡ FALLBACK CASE (Circuit Breaker)
    @Test
    @DisplayName("GET /cart/total — returns fallback when price service fails")
    void total_shouldReturnFallback() throws Exception {

        PriceResponseDto fallback =
                new PriceResponseDto("P1", BigDecimal.ZERO, "USD", "FALLBACK");

        when(priceClientService.getPrice("P1")).thenReturn(fallback);

        mockMvc.perform(get("/api/v1/cart/user-001/total")
                        .header("Authorization", "Bearer unit-test-token")
                        .param("sku", "P1"))
                .andExpect(status().isOk())
                // FIX: Correct nested path
                .andExpect(jsonPath("$.data.priceResponse.priceSource").value("FALLBACK"))
                .andExpect(jsonPath("$.data.priceResponse.price").value(0));
    }

    // ❌ FAILURE CASE (UNHANDLED ERROR)
    @Test
    @DisplayName("GET /cart/total — returns 500 when unexpected error occurs")
    void total_shouldReturn500WhenError() throws Exception {

        when(priceClientService.getPrice("P1"))
                .thenThrow(new RuntimeException("Unexpected failure"));

        mockMvc.perform(get("/api/v1/cart/user-001/total")
                        .header("Authorization", "Bearer unit-test-token")
                        .param("sku", "P1"))
                .andExpect(status().is5xxServerError());
    }

    // ❌ VALIDATION CASE
    @Test
    @DisplayName("GET /cart/total — returns 400 when SKU missing")
    void total_shouldReturn400WhenSkuMissing() throws Exception {

        mockMvc.perform(get("/api/v1/cart/user-001/total")
                        .header("Authorization", "Bearer unit-test-token"))
                .andExpect(status().isBadRequest());
    }
}