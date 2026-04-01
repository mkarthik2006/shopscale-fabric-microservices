package com.shopscale.price.controller;
import com.shopscale.common.dto.PriceResponseDto;


import com.shopscale.common.dto.StandardResponse;
import com.shopscale.price.service.PriceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Price Service API", description = "Endpoints for managing product pricing")
public class PriceController {

    private final PriceService priceService;

    @GetMapping(value = "/{sku}", produces = "application/json")
    @Operation(
            summary = "Fetch Price by SKU",
            description = "Retrieves real-time pricing for a product. SKU must not be blank."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pricing retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request: Blank SKU provided"),
            @ApiResponse(responseCode = "404", description = "Pricing not found for this SKU")
    })
    public ResponseEntity<StandardResponse<PriceResponseDto>> getPrice(
            @PathVariable("sku") @NotBlank(message = "SKU cannot be blank") String sku) {

        log.info("Fetching price for SKU: {}", sku);

        PriceResponseDto priceData = priceService.getPrice(sku);

        return ResponseEntity.ok(
                StandardResponse.success(priceData)
        );
    }
}