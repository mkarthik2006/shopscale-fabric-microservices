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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validates high-concurrency order placement with virtual-thread executor (PROJECT_RULES.md §1).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceConcurrencyTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Test
    @DisplayName("Concurrent placeOrder invocations each publish a distinct Kafka event (virtual-thread executor)")
    void placeOrder_underConcurrency_publishesOneEventPerOrder() throws Exception {
        ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
        OrderService orderService = new OrderService(repository, kafkaTemplate, vtExecutor);
        ReflectionTestUtils.setField(orderService, "topic", "order.placed");

        int threads = 32;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger idSeq = new AtomicInteger();

        when(repository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity o = invocation.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            if (o.getCreatedAt() == null) {
                o.setCreatedAt(java.time.Instant.now());
            }
            return o;
        });

        for (int i = 0; i < threads; i++) {
            int idx = i;
            vtExecutor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    OrderItemEmbeddable item = new OrderItemEmbeddable();
                    item.setSku("P" + idx);
                    item.setQuantity(1);
                    item.setUnitPrice(new BigDecimal("10.00"));
                    OrderEntity order = new OrderEntity();
                    order.setUserId("U-" + idSeq.incrementAndGet());
                    order.setTotalAmount(new BigDecimal("10.00"));
                    order.setCurrency("USD");
                    order.setItems(List.of(item));
                    orderService.placeOrder(order);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();

        verify(repository, times(threads)).save(any(OrderEntity.class));
        // placeOrder returns before virtual-thread async send; allow time for processOrderAsync
        verify(kafkaTemplate, timeout(30_000).times(threads))
                .send(eq("order.placed"), any(String.class), any(OrderPlacedEvent.class));

        vtExecutor.shutdown();
        assertThat(vtExecutor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
}
