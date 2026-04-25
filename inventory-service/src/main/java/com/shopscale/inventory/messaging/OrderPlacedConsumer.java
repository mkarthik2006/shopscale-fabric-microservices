package com.shopscale.inventory.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.CompensationOutboxEntity;
import com.shopscale.inventory.model.CompensationOutboxStatus;
import com.shopscale.inventory.model.InboxEventEntity;
import com.shopscale.inventory.model.InboxEventStatus;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.repository.CompensationOutboxRepository;
import com.shopscale.inventory.repository.InboxEventRepository;
import com.shopscale.inventory.repository.InventoryRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component
public class OrderPlacedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final InventoryRepository inventoryRepository;
    private final InboxEventRepository inboxEventRepository;
    private final CompensationOutboxRepository compensationOutboxRepository;

    public OrderPlacedConsumer(
            InventoryRepository inventoryRepository,
            InboxEventRepository inboxEventRepository,
            CompensationOutboxRepository compensationOutboxRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inboxEventRepository = inboxEventRepository;
        this.compensationOutboxRepository = compensationOutboxRepository;
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
        if (inbox.getStatus() != InboxEventStatus.RECEIVED) {
            log.info("OrderPlacedEvent already claimed by another worker | eventId={} status={}", eventId, inbox.getStatus());
            return;
        }
        int claimed = inboxEventRepository.updateStatusIfCurrent(eventId, InboxEventStatus.RECEIVED, InboxEventStatus.IN_PROGRESS);
        if (claimed == 0) {
            log.info("OrderPlacedEvent claim failed, skipping duplicate processing | eventId={}", eventId);
            return;
        }
        inbox.setStatus(InboxEventStatus.IN_PROGRESS);

        log.info("Processing inventory | orderId={}", event.orderId());

        try {
            Map<String, InventoryEntity> inventoryMap = new HashMap<>();


            for (OrderPlacedEvent.Item item : event.items()) {

                InventoryEntity inv = inventoryRepository.findById(item.sku())
                        .orElseThrow(() -> new RuntimeException("SKU not found: " + item.sku()));

                if (inv.getStock() < item.quantity()) {
                    enqueueCompensation(event, item.sku(), "INSUFFICIENT_STOCK");
                    markProcessed(inbox);
                    return;
                }

                inventoryMap.put(item.sku(), inv);
            }


            for (OrderPlacedEvent.Item item : event.items()) {
                InventoryEntity inv = inventoryMap.get(item.sku());
                inv.setStock(inv.getStock() - item.quantity());
                inventoryRepository.save(inv);
            }

            markProcessed(inbox);

            log.info("Inventory reserved successfully | orderId={}", event.orderId());

        } catch (Exception e) {
            log.error("Inventory processing failed with transient/system error | orderId={} | eventId={}",
                    event.orderId(), event.eventId(), e);

            throw e;
        }
    }

    /**
     * SAGA Compensation Event
     */
    private void enqueueCompensation(OrderPlacedEvent event, String sku, String reason) {
        log.warn("SAGA compensation enqueued | orderId={} | reason={}", event.orderId(), reason);
        CompensationOutboxEntity outbox = new CompensationOutboxEntity();
        outbox.setOrderId(event.orderId());
        outbox.setSourceEventId(event.eventId());
        outbox.setSku(sku);
        outbox.setReason(reason);
        outbox.setStatus(CompensationOutboxStatus.PENDING);
        compensationOutboxRepository.save(outbox);
    }

    private void markProcessed(InboxEventEntity inbox) {
        inbox.setStatus(InboxEventStatus.PROCESSED);
        inbox.setProcessedAt(java.time.Instant.now());
        inboxEventRepository.save(inbox);
    }
}