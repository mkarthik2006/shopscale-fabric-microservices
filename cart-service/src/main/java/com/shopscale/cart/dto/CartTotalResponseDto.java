package com.shopscale.cart.dto;

import com.shopscale.common.dto.PriceResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Cart Total Data", description = "Aggregated cart details")
public record CartTotalResponseDto(

        @Schema(description = "User ID", example = "U-90210")
        String userId,

        @Schema(description = "Product SKU", example = "P1")
        String sku,

        @Schema(description = "Resolved pricing details")
        PriceResponseDto priceResponse
) {}