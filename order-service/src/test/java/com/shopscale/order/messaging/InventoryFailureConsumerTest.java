package com.shopscale.order.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class InventoryFailureConsumerTest {

    private static final String TEST_TOPIC = "order.cancelled";

    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private InventoryFailureConsumer consumer;

    @BeforeEach
    void setUp() {
        // Clean constructor injection (enterprise best practice)
        consumer = new InventoryFailureConsumer(orderRepository, kafkaTemplate, TEST_TOPIC);
    }

    @Test
    @DisplayName("handleFailure - cancels PLACED order and emits valid compensation event")
    void handleFailure_shouldCancelAndEmitValidEvent() {
        UUID orderId = UUID.randomUUID();

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStatus("PLACED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Mock async Kafka success
        when(kafkaTemplate.send(eq(TEST_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "SKU_PRO_1", "Insufficient stock"
        );

        consumer.handleFailure(event);

        // ✅ Verify state change
        assertThat(order.getStatus()).isEqualTo("CANCELLED");

        // ✅ Verify DB interaction
        verify(orderRepository).save(order);

        // ✅ Verify correct event payload
        verify(kafkaTemplate).send(
                eq(TEST_TOPIC),
                eq(orderId.toString()),
                argThat(payload -> {
                    assertThat(payload).isInstanceOf(OrderCancelledEvent.class);
                    OrderCancelledEvent evt = (OrderCancelledEvent) payload;
                    return evt.orderId().equals(orderId);
                })
        );
    }

    @Test
    @DisplayName("handleFailure - idempotency guard skips already CANCELLED orders")
    void handleFailure_shouldSkipAlreadyCancelled() {
        UUID orderId = UUID.randomUUID();

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStatus("CANCELLED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "SKU_PRO_1", "Duplicate event"
        );

        consumer.handleFailure(event);

        // ✅ No duplicate processing
        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
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

        // ✅ No interaction with Kafka
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("handleFailure - handles Kafka send failure (resilience test)")
    void handleFailure_shouldHandleKafkaFailure() {
        UUID orderId = UUID.randomUUID();

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStatus("PLACED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Simulate Kafka failure
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(eq(TEST_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(failedFuture);

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "SKU_PRO_1", "Failure scenario"
        );

        try {
            consumer.handleFailure(event);
        } catch (RuntimeException ignored) {
            // Expected due to Kafka failure
        }

        // ✅ Order still cancelled (transaction completed)
        assertThat(order.getStatus()).isEqualTo("CANCELLED");

        verify(orderRepository).save(order);
    }
}