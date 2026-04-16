package com.shopscale.inventory.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.InboxEventEntity;
import com.shopscale.inventory.model.InboxEventStatus;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.repository.InboxEventRepository;
import com.shopscale.inventory.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class OrderPlacedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final InventoryRepository inventoryRepository;
    private final InboxEventRepository inboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String failureTopic;

    public OrderPlacedConsumer(
            InventoryRepository inventoryRepository,
            InboxEventRepository inboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topic.inventory-failure:inventory.failure}") String failureTopic) {
        this.inventoryRepository = inventoryRepository;
        this.inboxEventRepository = inboxEventRepository;
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

        Optional<InboxEventEntity> existing = inboxEventRepository.findById(eventId);
        if (existing.isPresent() && existing.get().getStatus() == InboxEventStatus.PROCESSED) {
            log.info("Duplicate OrderPlacedEvent skipped | eventId={}", eventId);
            return;
        }
        InboxEventEntity inbox = existing.orElseGet(() ->
                inboxEventRepository.save(new InboxEventEntity(eventId, event.eventType(), InboxEventStatus.RECEIVED))
        );

        log.info("Processing inventory | orderId={}", event.orderId());

        try {
            Map<String, InventoryEntity> inventoryMap = new HashMap<>();

            // ✅ STEP 1: VALIDATE ALL ITEMS (NO PARTIAL UPDATE)
            for (OrderPlacedEvent.Item item : event.items()) {

                InventoryEntity inv = inventoryRepository.findById(item.sku())
                        .orElseThrow(() -> new RuntimeException("SKU not found: " + item.sku()));

                if (inv.getStock() < item.quantity()) {
                    if (triggerCompensation(event, item.sku(), "INSUFFICIENT_STOCK")) {
                        markProcessed(inbox);
                    }
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

            markProcessed(inbox);

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
    private boolean triggerCompensation(OrderPlacedEvent event, String sku, String reason) {

        log.warn("❌ SAGA Compensation Triggered | orderId={} | reason={}", event.orderId(), reason);

        InventoryInsufficientEvent failureEvent = new InventoryInsufficientEvent(
                event.orderId(),
                event.eventId(),
                sku,
                reason
        );

        try {
            kafkaTemplate.send(failureTopic, event.orderId().toString(), failureEvent)
                    .get(5, TimeUnit.SECONDS);
            log.info("Compensation event published | topic={} | orderId={}", failureTopic, event.orderId());
            return true;
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to publish compensation event | orderId={}", event.orderId(), ex);
            return false;
        }
    }

    private void markProcessed(InboxEventEntity inbox) {
        inbox.setStatus(InboxEventStatus.PROCESSED);
        inbox.setProcessedAt(java.time.Instant.now());
        inboxEventRepository.save(inbox);
    }
}