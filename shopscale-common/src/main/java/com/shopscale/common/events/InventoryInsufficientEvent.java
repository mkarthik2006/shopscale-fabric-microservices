package com.shopscale.common.events;

import java.util.UUID;

public record InventoryInsufficientEvent(
    UUID orderId,
    UUID eventId,
    String sku,
    String reason
) {}
