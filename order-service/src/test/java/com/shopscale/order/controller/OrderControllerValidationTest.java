package com.shopscale.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopscale.common.exception.GlobalExceptionHandler;
import com.shopscale.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerValidationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        OrderController controller = new OrderController(mock(OrderService.class));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("Invalid create-order payload returns 400")
    void invalidPayloadReturns400() throws Exception {
        String invalidPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", "",
                "currency", "usd",
                "totalAmount", 0,
                "items", java.util.List.of(java.util.Map.of(
                        "sku", "",
                        "quantity", 0,
                        "unitPrice", 0
                ))
        ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
