package com.shopscale.order.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryFailureConsumerTest {

    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private InventoryFailureConsumer consumer;

    @Test
    @DisplayName("handleFailure - cancels a PLACED order on inventory shortage")
    void handleFailure_shouldCancelPlacedOrder() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStatus("PLACED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "P1", "Insufficient stock"
        );

        consumer.handleFailure(event);

        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("handleFailure - does not cancel an already shipped order")
    void handleFailure_shouldNotCancelNonPlacedOrder() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStatus("SHIPPED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                orderId, UUID.randomUUID(), "P1", "Insufficient stock"
        );

        consumer.handleFailure(event);

        assertThat(order.getStatus()).isEqualTo("SHIPPED");
        verify(orderRepository, never()).save(any());
    }
}