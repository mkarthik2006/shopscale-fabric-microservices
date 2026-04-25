package com.shopscale.order.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.common.events.OrderCancelledEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.repository.OrderRepository;
import com.shopscale.order.repository.OutboxEventRepository;
import com.shopscale.order.service.OrderOutboxMapper;
import org.springframework.kafka.annotation.KafkaListener;
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
    private final OutboxEventRepository outboxEventRepository;
    private final OrderOutboxMapper outboxMapper;

    public InventoryFailureConsumer(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            OrderOutboxMapper outboxMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxMapper = outboxMapper;
    }

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic.inventory-failure:inventory.failure}",
            groupId = "order-group"
    )
    public void handleFailure(InventoryInsufficientEvent event) {

        log.warn("Inventory failure | orderId={} | reason={}", event.orderId(), event.reason());

        Optional<OrderEntity> orderOpt = orderRepository.findById(event.orderId());


        if (orderOpt.isEmpty()) {
            log.error("Order {} not found for inventory failure event", event.orderId());
            return;
        }

        OrderEntity order = orderOpt.get();


        if ("CANCELLED".equals(order.getStatus())) {
            log.warn("Duplicate event detected for order {}, skipping", order.getId());
            return;
        }


        if ("PLACED".equals(order.getStatus())) {

            order.setStatus("CANCELLED");
            orderRepository.save(order);


            OrderCancelledEvent cancelledEvent = buildCompensationEvent(order, event.reason());


            OutboxEventEntity outboxEvent = new OutboxEventEntity();
            outboxEvent.setAggregateType("ORDER");
            outboxEvent.setAggregateId(order.getId());
            outboxEvent.setEventType("ORDER_CANCELLED");
            outboxEvent.setPayload(outboxMapper.toPayload(cancelledEvent));
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEventRepository.save(outboxEvent);
            log.info("Order cancellation persisted to outbox | orderId={} outboxId={}", order.getId(), outboxEvent.getId());

        } else {
            log.warn("Order {} in state {}, skipping cancellation.", order.getId(), order.getStatus());
        }
    }

    private OrderCancelledEvent buildCompensationEvent(OrderEntity order, String reason) {
        if (order.getUserEmail() == null || order.getUserEmail().isBlank() || !order.getUserEmail().contains("@")) {
            throw new IllegalStateException("Order is missing a valid userEmail for compensation event");
        }
        return new OrderCancelledEvent(
                UUID.randomUUID(),
                "ORDER_CANCELLED",
                Instant.now(),
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                reason,
                order.getTotalAmount(),
                order.getCurrency()
        );
    }
}