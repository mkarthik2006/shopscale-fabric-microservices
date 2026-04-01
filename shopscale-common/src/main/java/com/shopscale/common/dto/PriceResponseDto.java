package com.shopscale.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
        name = "Price Response Model",
        description = "Standard Shared DTO representing the live pricing attributes for a specific product SKU."
)
public record PriceResponseDto(

        @Schema(description = "The unique Stock Keeping Unit (SKU) identifier.", example = "P1")
        String sku,

        @Schema(description = "The real-time resolved price of the product.", example = "199.99")
        BigDecimal price,

        @Schema(description = "The standard 3-letter currency code.", example = "USD")
        String currency,

        @Schema(description = "Indicates whether the price was fetched 'LIVE' or 'CACHED'.", example = "LIVE")
        String priceSource

) {}
