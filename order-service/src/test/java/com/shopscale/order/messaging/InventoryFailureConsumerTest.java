package com.shopscale.order.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.repository.OrderRepository;
import com.shopscale.order.repository.OutboxEventRepository;
import com.shopscale.order.service.OrderOutboxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class InventoryFailureConsumerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private OrderOutboxMapper outboxMapper;

    private InventoryFailureConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryFailureConsumer(orderRepository, outboxEventRepository, outboxMapper);
    }

    @Test
    @DisplayName("handleFailure - cancels PLACED order and emits valid compensation event")
    void handleFailure_shouldCancelAndEmitValidEvent() {
        UUID orderId = UUID.randomUUID();

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStatus("PLACED");
        order.setUserEmail("user-1@shopscale.dev");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(outboxMapper.toPayload(any(OrderCancelledEvent.class))).thenReturn("{\"eventType\":\"ORDER_CANCELLED\"}");

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "SKU_PRO_1", "Insufficient stock"
        );

        consumer.handleFailure(event);

        // ✅ Verify state change
        assertThat(order.getStatus()).isEqualTo("CANCELLED");

        // ✅ Verify DB interaction
        verify(orderRepository).save(order);

        verify(outboxMapper).toPayload(argThat((OrderCancelledEvent evt) -> evt.orderId().equals(orderId)));
        verify(outboxEventRepository).save(argThat(outbox ->
                outbox.getAggregateId().equals(orderId)
                        && "ORDER".equals(outbox.getAggregateType())
                        && "ORDER_CANCELLED".equals(outbox.getEventType())
                        && outbox.getStatus() == OutboxStatus.PENDING
                        && outbox.getPayload() != null
                        && !outbox.getPayload().isBlank()
        ));
    }

    @Test
    @DisplayName("handleFailure - idempotency guard skips already CANCELLED orders")
    void handleFailure_shouldSkipAlreadyCancelled() {
        UUID orderId = UUID.randomUUID();

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStatus("CANCELLED");
        order.setUserEmail("user-1@shopscale.dev");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "SKU_PRO_1", "Duplicate event"
        );

        consumer.handleFailure(event);

        // ✅ No duplicate processing
        verify(orderRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleFailure - handles missing order gracefully")
    void handleFailure_shouldHandleMissingOrder() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "SKU_PRO_1", "No stock"
        );

        consumer.handleFailure(event);

        verify(outboxEventRepository, never()).save(any());
    }
}