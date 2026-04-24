package com.shopscale.order.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Invalid payload should fail bean validation constraints")
    void invalidPayloadShouldFailValidation() {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId("");
        dto.setCurrency("usd");

        OrderRequestDto.ItemDto item = new OrderRequestDto.ItemDto();
        item.setSku("");
        item.setQuantity(0);
        dto.setItems(List.of(item));

        var violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }
}
