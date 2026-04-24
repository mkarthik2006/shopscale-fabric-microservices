package com.shopscale.order.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;
@Data
public class OrderRequestDto {
    // Populated from authenticated principal on the server side.
    private String userId;
    // Populated from authenticated principal on the server side.
    private String userEmail;
    @NotBlank(message = "currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be an ISO-4217 code")
    private String currency;
    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<ItemDto> items;
    @Data
    public static class ItemDto {
        @NotBlank(message = "sku is required")
        private String sku;
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;
    }
}