package com.shopscale.order.service;

import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.common.events.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Observability-only SAGA orchestrator. Keeps saga monitoring/audit concerns
 * decoupled from the write-side consumers (inventory, notification, etc.).
 *
 * Listens on the real event topics used by the system:
 *   - order.placed     -> saga start marker
 *   - order.cancelled  -> saga compensation completion marker
 *
 * NOTE: This bean deliberately does NOT mutate state; it exists for tracing
 * and audit logs only. All business state transitions happen in the dedicated
 * write-side consumers.
 */
@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    @KafkaListener(topics = "${app.kafka.topic.order-placed:order.placed}", groupId = "order-saga-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("SAGA START | orderId={} traceId={} spanId={}",
                event.orderId(),
                MDC.get("traceId"),
                MDC.get("spanId"));
    }

    @KafkaListener(topics = "${app.kafka.topic.order-cancelled:order.cancelled}", groupId = "order-saga-group")
    public void handleCompensation(OrderCancelledEvent event) {
        log.warn("SAGA COMPENSATION | orderId={} reason={} traceId={} spanId={}",
                event.orderId(),
                event.reason(),
                MDC.get("traceId"),
                MDC.get("spanId"));
    }
}