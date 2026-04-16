package com.shopscale.order.service;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.common.events.CompensationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    // Observability-only listener: keeps SAGA monitoring decoupled from write-side consumers.
    @KafkaListener(topics = "order.placed", groupId = "order-saga-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("SAGA START | orderId={} traceId={} spanId={}",
                event.orderId(),
                MDC.get("traceId"),
                MDC.get("spanId"));
    }

    @KafkaListener(topics = "order.compensation", groupId = "order-saga-group")
    public void handleCompensation(CompensationEvent event) {
        log.warn("SAGA COMPENSATION | orderId={} traceId={} spanId={}",
                event.orderId(),
                MDC.get("traceId"),
                MDC.get("spanId"));
    }
}