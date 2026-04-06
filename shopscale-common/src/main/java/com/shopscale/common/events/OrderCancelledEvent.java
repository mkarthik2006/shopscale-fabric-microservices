package com.shopscale.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SAGA Compensating Event (Project Doc Page 6).
 * Published by Order Service when an order is CANCELLED due to inventory failure.
 * Consumed by Notification Service to email the customer about cancellation.
 */
public record OrderCancelledEvent(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    UUID orderId,
    String userId,
    String reason,
    BigDecimal totalAmount,
    String currency
) {}