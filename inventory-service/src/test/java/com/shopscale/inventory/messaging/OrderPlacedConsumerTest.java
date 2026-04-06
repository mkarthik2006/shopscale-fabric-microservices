package com.shopscale.inventory.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.model.ProcessedEventEntity;
import com.shopscale.inventory.repository.InventoryRepository;
import com.shopscale.inventory.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    // FIX: Use manual construction instead of @InjectMocks (constructor has @Value param)
    private OrderPlacedConsumer consumer;

    private OrderPlacedEvent event;
    private UUID eventId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        // FIX: Manually construct with failureTopic value
        consumer = new OrderPlacedConsumer(
                inventoryRepository, processedEventRepository, kafkaTemplate, "inventory.failure"
        );

        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        event = new OrderPlacedEvent(
                eventId, "ORDER_PLACED", Instant.now(),
                orderId, "U-001",
                List.of(new OrderPlacedEvent.Item("P1", 2, new BigDecimal("199.99"))),
                new BigDecimal("399.98"), "USD"
        );
    }

    @Test
    @DisplayName("consume — deducts stock and marks event as SUCCESS")
    void consume_shouldDeductStockSuccessfully() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(100);

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        consumer.consume(event);

        assertThat(inv.getStock()).isEqualTo(98);
        verify(inventoryRepository).save(inv);

        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("consume — skips duplicate events (idempotency guard)")
    void consume_shouldSkipDuplicateEvent() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        consumer.consume(event);

        verify(inventoryRepository, never()).findById(any());
        verify(inventoryRepository, never()).save(any());
        // FIX: verify 3-arg send() signature
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("consume — publishes InventoryInsufficientEvent when stock is too low")
    void consume_shouldPublishFailureWhenInsufficientStock() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(1); // less than requested quantity of 2

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        // FIX: Mock 3-arg Kafka send with CompletableFuture
        when(kafkaTemplate.send(eq("inventory.failure"), anyString(), any(InventoryInsufficientEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        consumer.consume(event);

        // FIX: verify 3-arg send() call
        verify(kafkaTemplate).send(eq("inventory.failure"), anyString(), any(InventoryInsufficientEvent.class));

        // Stock NOT deducted
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume — publishes failure when SKU not found in inventory")
    void consume_shouldPublishFailureWhenSkuNotFound() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(inventoryRepository.findById("P1")).thenReturn(Optional.empty());

        // FIX: Mock 3-arg Kafka send
        when(kafkaTemplate.send(eq("inventory.failure"), anyString(), any(InventoryInsufficientEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        consumer.consume(event);

        // FIX: verify 3-arg send() call with captor
        ArgumentCaptor<InventoryInsufficientEvent> failCaptor =
                ArgumentCaptor.forClass(InventoryInsufficientEvent.class);
        verify(kafkaTemplate).send(eq("inventory.failure"), anyString(), failCaptor.capture());

        assertThat(failCaptor.getValue().orderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("consume — processes multi-item orders correctly")
    void consume_shouldProcessMultiItemOrder() {
        OrderPlacedEvent multiEvent = new OrderPlacedEvent(
                eventId, "ORDER_PLACED", Instant.now(),
                orderId, "U-002",
                List.of(
                        new OrderPlacedEvent.Item("P1", 3, new BigDecimal("199.99")),
                        new OrderPlacedEvent.Item("P2", 5, new BigDecimal("89.50"))
                ),
                new BigDecimal("1047.47"), "USD"
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

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