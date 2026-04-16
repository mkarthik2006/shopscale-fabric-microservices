package com.shopscale.inventory.dto;

public record InventoryResponseDto(
    String sku,
    Integer stock
) {}
