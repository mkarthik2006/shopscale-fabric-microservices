package com.shopscale.order.service;

import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.repository.OrderRepository;
import com.shopscale.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox path: order transaction succeeds without direct Kafka dependency.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceAsyncFailureTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderOutboxMapper outboxMapper;

    @Test
    @DisplayName("placeOrder persists order and outbox event in same request")
    void placeOrder_persistsOutboxInSameTransactionBoundary() {
        OrderService orderService = new OrderService(repository, outboxEventRepository, outboxMapper);

        OrderItemEmbeddable item = new OrderItemEmbeddable();
        item.setSku("P1");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("9.99"));
        OrderEntity order = new OrderEntity();
        order.setUserId("U-1");
        order.setTotalAmount(new BigDecimal("9.99"));
        order.setCurrency("USD");
        order.setItems(List.of(item));

        UUID id = UUID.randomUUID();
        when(repository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setId(id);
            o.setCreatedAt(java.time.Instant.now());
            return o;
        });
        when(outboxMapper.toPayload(any(OrderEntity.class))).thenReturn("{\"eventType\":\"ORDER_PLACED\"}");

        OrderEntity saved = orderService.placeOrder(order);
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getStatus()).isEqualTo("PLACED");

        verify(repository, times(1)).save(any(OrderEntity.class));
        verify(outboxEventRepository, times(1)).save(any());
    }
}
