package com.shopscale.cart.controller;

import com.shopscale.cart.service.PriceClientService;
import com.shopscale.cart.dto.CartTotalResponseDto;
import com.shopscale.common.dto.StandardResponse;
import com.shopscale.common.dto.PriceResponseDto;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/cart", "/api/v1/cart"})
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Cart Service API", description = "Cart operations and pricing aggregation")
public class CartController {

    private final PriceClientService priceClientService;

    public record CartRequest(@NotBlank String userId, @NotBlank String sku) {}

    @GetMapping(produces = "application/json")
    public ResponseEntity<StandardResponse<CartTotalResponseDto>> totalByQuery(
            @RequestParam @NotBlank(message = "User ID cannot be blank") String userId,
            @RequestParam @NotBlank(message = "SKU cannot be blank") String sku) {
        return total(userId, sku);
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<StandardResponse<CartTotalResponseDto>> totalByBody(
            @Valid @RequestBody CartRequest request) {
        return total(request.userId(), request.sku());
    }

    @GetMapping(value = "/{userId}/total", produces = "application/json")
    @Operation(
            summary = "Calculate Cart Total",
            description = "Fetches live price from Price Service and builds cart summary"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Price not found"),
            @ApiResponse(responseCode = "500", description = "Upstream failure")
    })
    public ResponseEntity<StandardResponse<CartTotalResponseDto>> total(
            @PathVariable @NotBlank(message = "User ID cannot be blank") String userId,
            @RequestParam @NotBlank(message = "SKU cannot be blank") String sku) {

        log.info("Calculating cart total for user: {}, sku: {}", userId, sku);

        PriceResponseDto priceData = priceClientService.getPrice(sku);

        CartTotalResponseDto cartSummary = new CartTotalResponseDto(
                userId,
                sku,
                priceData
        );

        return ResponseEntity.ok(
                StandardResponse.success("Cart total retrieved successfully", cartSummary)
        );
    }
}