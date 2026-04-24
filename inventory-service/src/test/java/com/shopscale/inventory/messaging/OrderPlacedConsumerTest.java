package com.shopscale.inventory.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.CompensationOutboxEntity;
import com.shopscale.inventory.model.InboxEventEntity;
import com.shopscale.inventory.model.InboxEventStatus;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.repository.CompensationOutboxRepository;
import com.shopscale.inventory.repository.InboxEventRepository;
import com.shopscale.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for OrderPlacedConsumer (Inventory Service)
 * Doc Ref: Week 2 — "Inventory Service consumes, updates stock"
 * Doc Ref: Page 6 — "enable.idempotence: true"
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class OrderPlacedConsumerTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InboxEventRepository inboxEventRepository;
    @Mock private CompensationOutboxRepository compensationOutboxRepository;

    // FIX: Use manual construction to control dependencies explicitly.
    private OrderPlacedConsumer consumer;

    private OrderPlacedEvent event;
    private UUID eventId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        consumer = new OrderPlacedConsumer(
                inventoryRepository, inboxEventRepository, compensationOutboxRepository
        );
        org.mockito.Mockito.lenient().when(inboxEventRepository.updateStatusIfCurrent(
                any(UUID.class), eq(InboxEventStatus.RECEIVED), eq(InboxEventStatus.IN_PROGRESS)))
                .thenReturn(1);

        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        event = new OrderPlacedEvent(
                eventId, "ORDER_PLACED", Instant.now(),
                orderId, "U-001", "u001@shopscale.dev",
                List.of(new OrderPlacedEvent.Item("P1", 2, new BigDecimal("199.99"))),
                new BigDecimal("399.98"), "USD"
        );
    }

    @Test
    @DisplayName("consume — deducts stock and marks event as SUCCESS")
    void consume_shouldDeductStockSuccessfully() {
        when(inboxEventRepository.findById(eventId)).thenReturn(Optional.empty());
        when(inboxEventRepository.save(any(InboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(100);

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        consumer.consume(event);

        assertThat(inv.getStock()).isEqualTo(98);
        verify(inventoryRepository).save(inv);

        ArgumentCaptor<InboxEventEntity> captor = ArgumentCaptor.forClass(InboxEventEntity.class);
        verify(inboxEventRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getEventId()).isEqualTo(eventId);
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("consume — skips duplicate events (idempotency guard)")
    void consume_shouldSkipDuplicateEvent() {
        InboxEventEntity processed = new InboxEventEntity(eventId, "ORDER_PLACED", InboxEventStatus.PROCESSED);
        when(inboxEventRepository.findById(eventId)).thenReturn(Optional.of(processed));

        consumer.consume(event);

        verify(inventoryRepository, never()).findById(any());
        verify(inventoryRepository, never()).save(any());
        verify(compensationOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume — publishes InventoryInsufficientEvent when stock is too low")
    void consume_shouldPublishFailureWhenInsufficientStock() {
        when(inboxEventRepository.findById(eventId)).thenReturn(Optional.empty());
        when(inboxEventRepository.save(any(InboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(1); // less than requested quantity of 2

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        consumer.consume(event);

        verify(compensationOutboxRepository).save(any(CompensationOutboxEntity.class));

        // Stock NOT deducted
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume — rethrows when SKU not found to allow Kafka retry/DLQ")
    void consume_shouldRethrowWhenSkuNotFound() {
        when(inboxEventRepository.findById(eventId)).thenReturn(Optional.empty());
        when(inboxEventRepository.save(any(InboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.findById("P1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SKU not found");
        verify(compensationOutboxRepository, never()).save(any(CompensationOutboxEntity.class));
    }

    @Test
    @DisplayName("consume — processes multi-item orders correctly")
    void consume_shouldProcessMultiItemOrder() {
        OrderPlacedEvent multiEvent = new OrderPlacedEvent(
                eventId, "ORDER_PLACED", Instant.now(),
                orderId, "U-002", "u002@shopscale.dev",
                List.of(
                        new OrderPlacedEvent.Item("P1", 3, new BigDecimal("199.99")),
                        new OrderPlacedEvent.Item("P2", 5, new BigDecimal("89.50"))
                ),
                new BigDecimal("1047.47"), "USD"
        );

        when(inboxEventRepository.findById(eventId)).thenReturn(Optional.empty());
        when(inboxEventRepository.save(any(InboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryEntity invP1 = new InventoryEntity();
        invP1.setSku("P1");
        invP1.setStock(50);

        InventoryEntity invP2 = new InventoryEntity();
        invP2.setSku("P2");
        invP2.setStock(20);

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(invP1));
        when(inventoryRepository.findById("P2")).thenReturn(Optional.of(invP2));

        consumer.consume(multiEvent);

        assertThat(invP1.getStock()).isEqualTo(47);
        assertThat(invP2.getStock()).isEqualTo(15);
        verify(inventoryRepository, times(2)).save(any());
    }
}