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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPlacedConsumerTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderPlacedConsumer consumer;

    private OrderPlacedEvent event;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        event = new OrderPlacedEvent(
                eventId, "ORDER_PLACED", Instant.now(),
                UUID.randomUUID(), "U-001",
                List.of(new OrderPlacedEvent.Item("P1", 2, new BigDecimal("199.99"))),
                new BigDecimal("399.98"), "USD"
        );
    }

    @Test
    @DisplayName("consume - deducts stock and marks event as processed")
    void consume_shouldDeductStockSuccessfully() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(100);

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        consumer.consume(event);

        assertThat(inv.getStock()).isEqualTo(98);
        verify(inventoryRepository, times(2)).findById("P1");
        verify(inventoryRepository).save(inv);

        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("consume - skips duplicate events (idempotency)")
    void consume_shouldSkipDuplicateEvent() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        consumer.consume(event);

        verify(inventoryRepository, never()).findById(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume - publishes failure event when stock is insufficient")
    void consume_shouldPublishFailureWhenInsufficientStock() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        InventoryEntity inv = new InventoryEntity();
        inv.setSku("P1");
        inv.setStock(1); // only 1 in stock, order asks for 2

        when(inventoryRepository.findById("P1")).thenReturn(Optional.of(inv));

        consumer.consume(event);

        verify(kafkaTemplate).send(eq("inventory.failure"), any(InventoryInsufficientEvent.class));

        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED_NO_STOCK");
    }

    @Test
    @DisplayName("consume - publishes failure event when SKU not found")
    void consume_shouldPublishFailureWhenSkuNotFound() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(inventoryRepository.findById("P1")).thenReturn(Optional.empty());

        consumer.consume(event);

        verify(kafkaTemplate).send(eq("inventory.failure"), any(InventoryInsufficientEvent.class));
    }
}