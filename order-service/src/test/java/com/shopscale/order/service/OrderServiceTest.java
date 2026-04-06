package com.shopscale.order.service;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "topic", "order.placed");

        OrderItemEmbeddable item = new OrderItemEmbeddable();
        item.setSku("P1");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("199.99"));

        sampleOrder = new OrderEntity();
        sampleOrder.setUserId("U-001");
        sampleOrder.setTotalAmount(new BigDecimal("399.98"));
        sampleOrder.setCurrency("USD");
        sampleOrder.setItems(List.of(item));
    }

    @Test
    @DisplayName("placeOrder - saves order with PLACED status and publishes Kafka event")
    void placeOrder_shouldSaveAndPublishEvent() {
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

        OrderEntity result = orderService.placeOrder(sampleOrder);

        assertThat(result.getStatus()).isEqualTo("PLACED");
        assertThat(result.getId()).isNotNull();

        verify(repository, times(1)).save(any(OrderEntity.class));

        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("order.placed"), any(String.class), eventCaptor.capture());

        OrderPlacedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(result.getId());
        assertThat(event.userId()).isEqualTo("U-001");
        assertThat(event.eventType()).isEqualTo("ORDER_PLACED");
        assertThat(event.items()).hasSize(1);
        assertThat(event.items().get(0).sku()).isEqualTo("P1");
        assertThat(event.totalAmount()).isEqualByComparingTo(new BigDecimal("399.98"));
    }

    @Test
    @DisplayName("placeOrder - sets status to PLACED before saving")
    void placeOrder_shouldSetStatusToPlaced() {
        sampleOrder.setStatus("DRAFT");
        when(repository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            o.setCreatedAt(java.time.Instant.now());
            return o;
        });

        OrderEntity result = orderService.placeOrder(sampleOrder);
        assertThat(result.getStatus()).isEqualTo("PLACED");
    }

    @Test
    @DisplayName("getById - returns order when found")
    void getById_shouldReturnOrder() {
        UUID id = UUID.randomUUID();
        sampleOrder.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(sampleOrder));

        OrderEntity result = orderService.getById(id);
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("getById - throws when order not found")
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(id))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("byUser - returns list of user orders")
    void byUser_shouldReturnUserOrders() {
        when(repository.findByUserId("U-001")).thenReturn(List.of(sampleOrder));

        List<OrderEntity> result = orderService.byUser("U-001");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("U-001");
    }
}