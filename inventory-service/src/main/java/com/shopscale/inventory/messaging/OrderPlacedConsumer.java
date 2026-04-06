package com.shopscale.inventory.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.model.ProcessedEventEntity;
import com.shopscale.inventory.repository.InventoryRepository;
import com.shopscale.inventory.repository.ProcessedEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component
public class OrderPlacedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String failureTopic;

    public OrderPlacedConsumer(
            InventoryRepository inventoryRepository,
            ProcessedEventRepository processedEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topic.inventory-failure:inventory.failure}") String failureTopic) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.failureTopic = failureTopic;
    }

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.order-placed:order.placed}",
            groupId = "inventory-group"
    )
    public void consume(OrderPlacedEvent event) {

        UUID eventId = event.eventId();

        // ✅ IDEMPOTENCY (persistent)
        if (processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate OrderPlacedEvent skipped | eventId={}", eventId);
            return;
        }

        log.info("Processing inventory | orderId={}", event.orderId());

        try {
            Map<String, InventoryEntity> inventoryMap = new HashMap<>();

            // ✅ STEP 1: VALIDATE ALL ITEMS (NO PARTIAL UPDATE)
            for (OrderPlacedEvent.Item item : event.items()) {

                InventoryEntity inv = inventoryRepository.findById(item.sku())
                        .orElseThrow(() -> new RuntimeException("SKU not found: " + item.sku()));

                if (inv.getStock() < item.quantity()) {
                    triggerCompensation(event, item.sku(), "INSUFFICIENT_STOCK");
                    return;
                }

                inventoryMap.put(item.sku(), inv);
            }

            // ✅ STEP 2: APPLY STOCK DEDUCTION
            for (OrderPlacedEvent.Item item : event.items()) {
                InventoryEntity inv = inventoryMap.get(item.sku());
                inv.setStock(inv.getStock() - item.quantity());
                inventoryRepository.save(inv);
            }

            // ✅ MARK SUCCESS
            processedEventRepository.save(new ProcessedEventEntity(eventId, "SUCCESS"));

            log.info("Inventory reserved successfully | orderId={}", event.orderId());

        } catch (Exception e) {
            log.error("Inventory processing failed | orderId={}", event.orderId(), e);

            // ✅ SYSTEM FAILURE → COMPENSATION
            triggerCompensation(event, "SYSTEM", "PROCESSING_ERROR: " + e.getMessage());
        }
    }

    /**
     * SAGA Compensation Event
     */
    private void triggerCompensation(OrderPlacedEvent event, String sku, String reason) {

        log.warn("❌ SAGA Compensation Triggered | orderId={} | reason={}", event.orderId(), reason);

        InventoryInsufficientEvent failureEvent = new InventoryInsufficientEvent(
                event.orderId(),
                event.eventId(),
                sku,
                reason
        );

        // ✅ RELIABLE KAFKA SEND
        kafkaTemplate.send(failureTopic, event.orderId().toString(), failureEvent)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Compensation event published | topic={} | orderId={}", failureTopic, event.orderId());

                        processedEventRepository.save(
                                new ProcessedEventEntity(event.eventId(), "FAILED_" + reason)
                        );

                    } else {
                        log.error("CRITICAL: Failed to publish compensation event | orderId={}", event.orderId(), ex);
                        throw new RuntimeException("Kafka publish failed", ex);
                    }
                });
    }
}