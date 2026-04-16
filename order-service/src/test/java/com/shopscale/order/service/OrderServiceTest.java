package com.shopscale.order.service;

import com.shopscale.order.dto.OrderResponseDto;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.repository.OrderRepository;
import com.shopscale.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderOutboxMapper outboxMapper;

    @InjectMocks
    private OrderService orderService;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        OrderItemEmbeddable item = new OrderItemEmbeddable();
        item.setSku("P1");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("199.99"));

        sampleOrder = new OrderEntity();
        sampleOrder.setUserId("U-001");
        sampleOrder.setTotalAmount(new BigDecimal("399.98"));
        sampleOrder.setCurrency("USD");
        sampleOrder.setItems(List.of(item));

        lenient().when(outboxMapper.toPayload(any(OrderEntity.class))).thenReturn("{\"eventType\":\"ORDER_PLACED\"}");
    }

    @Test
    @DisplayName("placeOrder - saves order with PLACED status and persists outbox event")
    void placeOrder_shouldSaveAndPersistOutboxEvent() {
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

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getAggregateType()).isEqualTo("ORDER");
        assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo(result.getId());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("ORDER_PLACED");
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
        verify(outboxEventRepository, times(1)).save(any(OutboxEventEntity.class));
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

    @Test
    @DisplayName("byUserSummaries - returns projected DTO list")
    void byUserSummaries_shouldReturnProjectedDtos() {
        UUID id = UUID.randomUUID();
        when(repository.findOrderSummariesByUser("U-001"))
                .thenReturn(List.of(new OrderResponseDto(id, "PLACED", "U-001")));

        List<OrderResponseDto> result = orderService.byUserSummaries("U-001");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(id);
        assertThat(result.getFirst().getStatus()).isEqualTo("PLACED");
    }
}