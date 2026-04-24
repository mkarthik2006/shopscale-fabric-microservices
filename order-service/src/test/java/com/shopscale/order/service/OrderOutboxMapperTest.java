package com.shopscale.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderOutboxMapperTest {

    private final OrderOutboxMapper mapper = new OrderOutboxMapper(
            new ObjectMapper().registerModule(new JavaTimeModule())
    );

    @Test
    void orderPlacedPayload_shouldSerializeAndDeserialize() {
        OrderEntity order = buildOrder("user-1", "user-1@shopscale.dev");

        String payload = mapper.toPayload(order);
        OrderPlacedEvent event = mapper.fromPayload(payload);

        assertThat(event.eventType()).isEqualTo("ORDER_PLACED");
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.userEmail()).isEqualTo("user-1@shopscale.dev");
        assertThat(event.items()).hasSize(1);
        assertThat(event.totalAmount()).isEqualByComparingTo(new BigDecimal("19.99"));
    }

    @Test
    void orderPlacedPayload_shouldRejectInvalidEmail() {
        OrderEntity order = buildOrder("user-2", "invalid-email");

        assertThatThrownBy(() -> mapper.toPayload(order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing a valid userEmail");
    }

    @Test
    void cancelledPayload_shouldSerializeAndDeserialize() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                "ORDER_CANCELLED",
                Instant.now(),
                UUID.randomUUID(),
                "user-3",
                "user-3@shopscale.dev",
                "Inventory unavailable",
                new BigDecimal("29.99"),
                "USD"
        );

        String payload = mapper.toPayload(event);
        OrderCancelledEvent decoded = mapper.fromCancelledPayload(payload);

        assertThat(decoded.eventType()).isEqualTo("ORDER_CANCELLED");
        assertThat(decoded.userId()).isEqualTo("user-3");
        assertThat(decoded.reason()).isEqualTo("Inventory unavailable");
        assertThat(decoded.currency()).isEqualTo("USD");
    }

    private OrderEntity buildOrder(String userId, String userEmail) {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setCreatedAt(Instant.now());
        order.setUserId(userId);
        order.setUserEmail(userEmail);
        order.setCurrency("USD");
        order.setStatus("PLACED");
        order.setTotalAmount(new BigDecimal("19.99"));

        OrderItemEmbeddable item = new OrderItemEmbeddable();
        item.setSku("SKU-1");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("19.99"));
        order.setItems(List.of(item));
        return order;
    }
}
