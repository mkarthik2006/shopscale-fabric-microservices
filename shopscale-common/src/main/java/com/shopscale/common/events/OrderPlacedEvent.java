package com.shopscale.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    UUID orderId,
    String userId,
    List<Item> items,
    BigDecimal totalAmount,
    String currency
) {

    public record Item(
        String sku,
        int quantity,
        BigDecimal unitPrice
    ) {}
}