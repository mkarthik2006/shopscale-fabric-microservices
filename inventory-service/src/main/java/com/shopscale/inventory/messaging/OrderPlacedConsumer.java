package com.shopscale.inventory.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.model.ProcessedEventEntity;
import com.shopscale.inventory.repository.InventoryRepository;
import com.shopscale.inventory.repository.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component
public class OrderPlacedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderPlacedConsumer(InventoryRepository inventoryRepository,
                               ProcessedEventRepository processedEventRepository,
                               KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @KafkaListener(topics = "order.placed", groupId = "inventory-group")
    public void consume(OrderPlacedEvent event) {

        UUID eventId = event.eventId();

        
        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event skipped: {}", eventId);
            return;
        }

        log.info("Processing order event: {}", eventId);

       
        for (OrderPlacedEvent.Item item : event.items()) {
            InventoryEntity inv = inventoryRepository.findById(item.sku()).orElse(null);

            if (inv == null || inv.getStock() < item.quantity()) {
                handleFailure(event, item.sku(), "Item not found or insufficient stock");
                return; 
            }
        }

        
        for (OrderPlacedEvent.Item item : event.items()) {
            InventoryEntity inv = inventoryRepository.findById(item.sku()).get();
            inv.setStock(inv.getStock() - item.quantity());
            inventoryRepository.save(inv);
        }

       
        processedEventRepository.save(new ProcessedEventEntity(eventId, "SUCCESS"));
        log.info("Order event processed successfully: {}", eventId);
    }

    private void handleFailure(OrderPlacedEvent event, String sku, String reason) {
        log.warn("❌ Failed to process event {}: {}", event.eventId(), reason);

       
        kafkaTemplate.send("inventory.failure", new InventoryInsufficientEvent(
                event.orderId(), 
                event.eventId(),
                sku,
                reason
        ));

       
        processedEventRepository.save(new ProcessedEventEntity(event.eventId(), "FAILED_NO_STOCK"));
    }
}
