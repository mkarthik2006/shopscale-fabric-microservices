package com.shopscale.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.order.model.OrderEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OrderOutboxMapper {

    private final ObjectMapper objectMapper;

    public OrderOutboxMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toPayload(OrderEntity order) {
        try {
            return objectMapper.writeValueAsString(toOrderPlacedEvent(order));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }

    public OrderPlacedEvent fromPayload(String payload) {
        try {
            return objectMapper.readValue(payload, OrderPlacedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize outbox payload", ex);
        }
    }

    public String toPayload(OrderCancelledEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize order-cancelled outbox payload", ex);
        }
    }

    public OrderCancelledEvent fromCancelledPayload(String payload) {
        try {
            return objectMapper.readValue(payload, OrderCancelledEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize order-cancelled outbox payload", ex);
        }
    }

    private OrderPlacedEvent toOrderPlacedEvent(OrderEntity order) {
        if (order.getUserEmail() == null || order.getUserEmail().isBlank() || !order.getUserEmail().contains("@")) {
            throw new IllegalStateException("Order is missing a valid userEmail for event publishing");
        }
        List<OrderPlacedEvent.Item> eventItems = order.getItems().stream()
                .map(i -> new OrderPlacedEvent.Item(i.getSku(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        return new OrderPlacedEvent(
                UUID.randomUUID(),
                "ORDER_PLACED",
                order.getCreatedAt(),
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                eventItems,
                order.getTotalAmount(),
                order.getCurrency()
        );
    }
}
