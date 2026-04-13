package com.shopscale.order.service;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Failure-path: async Kafka publish throws — HTTP path still completes (decoupled saga).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceAsyncFailureTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Test
    @DisplayName("placeOrder returns saved order when async Kafka publish fails")
    void placeOrder_kafkaPublishFails_asyncDoesNotBreakHttpResponse() throws Exception {
        ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            OrderService orderService = new OrderService(repository, kafkaTemplate, vtExecutor);
            ReflectionTestUtils.setField(orderService, "topic", "order.placed");

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

            CountDownLatch kafkaInvoked = new CountDownLatch(1);
            when(kafkaTemplate.send(anyString(), anyString(), any(OrderPlacedEvent.class))).thenAnswer(inv -> {
                kafkaInvoked.countDown();
                throw new RuntimeException("broker down");
            });

            OrderEntity saved = orderService.placeOrder(order);
            assertThat(saved.getId()).isEqualTo(id);
            assertThat(saved.getStatus()).isEqualTo("PLACED");

            assertThat(kafkaInvoked.await(10, TimeUnit.SECONDS)).isTrue();
            verify(repository, times(1)).save(any(OrderEntity.class));
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(OrderPlacedEvent.class));
        } finally {
            vtExecutor.shutdown();
            vtExecutor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
