package com.shopscale.order.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryFailureConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryFailureConsumer.class);

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String cancelledTopic;

    public InventoryFailureConsumer(
            OrderRepository orderRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topic.order-cancelled:order.cancelled}") String cancelledTopic) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.cancelledTopic = cancelledTopic;
    }

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.inventory-failure:inventory.failure}",
            groupId = "order-group"
    )
    public void handleFailure(InventoryInsufficientEvent event) {

        log.warn("Inventory failure | orderId={} | reason={}", event.orderId(), event.reason());

        Optional<OrderEntity> orderOpt = orderRepository.findById(event.orderId());

        // ✅ Handle missing order (fault tolerance)
        if (orderOpt.isEmpty()) {
            log.error("Order {} not found for inventory failure event", event.orderId());
            return;
        }

        OrderEntity order = orderOpt.get();

        // ✅ IDEMPOTENCY GUARD (critical for Kafka at-least-once)
        if ("CANCELLED".equals(order.getStatus())) {
            log.warn("Duplicate event detected for order {}, skipping", order.getId());
            return;
        }

        // ✅ BUSINESS RULE
        if ("PLACED".equals(order.getStatus())) {

            order.setStatus("CANCELLED");
            orderRepository.save(order);

            // ✅ Build compensation event
            OrderCancelledEvent cancelledEvent = buildCompensationEvent(order, event.reason());

            // ✅ RELIABLE KAFKA PUBLISH (with failure handling)
            kafkaTemplate.send(cancelledTopic, order.getId().toString(), cancelledEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish OrderCancelledEvent for order {}", order.getId(), ex);
                            throw new RuntimeException("Kafka publish failed", ex);
                        } else {
                            log.info("OrderCancelledEvent published successfully for order {}", order.getId());
                        }
                    });

        } else {
            log.warn("Order {} in state {}, skipping cancellation.", order.getId(), order.getStatus());
        }
    }

    private OrderCancelledEvent buildCompensationEvent(OrderEntity order, String reason) {
        return new OrderCancelledEvent(
                UUID.randomUUID(),
                "ORDER_CANCELLED",
                Instant.now(),
                order.getId(),
                order.getUserId(),
                reason,
                order.getTotalAmount(),
                order.getCurrency()
        );
    }
}