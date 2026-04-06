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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for OrderPlacedConsumer (Inventory Service)
 * Doc Ref: Week 2 — "Inventory Service consumes, updates stock"
 * Doc Ref: Week 2 — "Kill the Inventory Service, place an order, restart it. The pending Kafka message must be processed successfully."
 * Doc Ref: Page 6 — "enable.idempotence: true — Prevents duplicate Order Placed events"
 */
@ExtendWith(MockitoExtension.class)
class OrderPlacedConsumerTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderPlacedConsumer consumer;

    private OrderPlacedEvent event;
    private UUID eventId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
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
        // Given: event not yet processed, inventory has sufficient stock
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(100);

        // findById called twice: once in validation loop, once in deduction loop
        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        // When
        consumer.consume(event);

        // Then: stock reduced by quantity (2)
        assertThat(inv.getStock()).isEqualTo(98);
        verify(inventoryRepository).save(inv);

        // Then: event marked as processed
        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("consume — skips duplicate events (idempotency guard)")
    void consume_shouldSkipDuplicateEvent() {
        // Given: event already processed
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        // When
        consumer.consume(event);

        // Then: no inventory lookup or save
        verify(inventoryRepository, never()).findById(any());
        verify(inventoryRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("consume — publishes InventoryInsufficientEvent when stock is too low")
    void consume_shouldPublishFailureWhenInsufficientStock() {
        // Given: event not processed, stock only 1 but need 2
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(1); // less than requested quantity of 2

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        // When
        consumer.consume(event);

        // Then: failure event sent to inventory.failure topic
        verify(kafkaTemplate).send(eq("inventory.failure"), any(InventoryInsufficientEvent.class));

        // Then: event marked as FAILED_NO_STOCK
        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED_NO_STOCK");

        // Then: stock NOT deducted (no save on inventory)
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume — publishes failure when SKU not found in inventory")
    void consume_shouldPublishFailureWhenSkuNotFound() {
        // Given: event not processed, SKU does not exist
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(inventoryRepository.findById("P1")).thenReturn(Optional.empty());

        // When
        consumer.consume(event);

        // Then: failure event published
        ArgumentCaptor<InventoryInsufficientEvent> failCaptor =
                ArgumentCaptor.forClass(InventoryInsufficientEvent.class);
        verify(kafkaTemplate).send(eq("inventory.failure"), failCaptor.capture());

        assertThat(failCaptor.getValue().orderId()).isEqualTo(orderId);
        assertThat(failCaptor.getValue().sku()).isEqualTo("P1");
    }

    @Test
    @DisplayName("consume — processes multi-item orders correctly")
    void consume_shouldProcessMultiItemOrder() {
        // Given: order with 2 different items
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

        // When
        consumer.consume(multiEvent);

        // Then: both stocks deducted
        assertThat(invP1.getStock()).isEqualTo(47);
        assertThat(invP2.getStock()).isEqualTo(15);
        verify(inventoryRepository, times(2)).save(any());
    }
}