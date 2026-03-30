package com.shopscale.order.messaging;

import com.shopscale.common.events.InventoryInsufficientEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.repository.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Component
public class InventoryFailureConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryFailureConsumer.class);

    private final OrderRepository orderRepository;

    public InventoryFailureConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    @KafkaListener(topics = "inventory.failure", groupId = "order-group")
    public void handleFailure(InventoryInsufficientEvent event) {
        log.warn("🚨 Received inventory failure notification for Order: {}. Reason: {}", event.orderId(), event.reason());

        Optional<OrderEntity> orderOpt = orderRepository.findById(event.orderId());

        if (orderOpt.isPresent()) {
            OrderEntity order = orderOpt.get();
            
            // Only cancel if it's currently in a non-final state (like PLACED)
            if ("PLACED".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                log.info("✅ Order {} has been successfully CANCELLED due to inventory shortage.", event.orderId());
            } else {
                log.warn("⚠️ Order {} is in state {}, skipping cancellation.", event.orderId(), order.getStatus());
            }
        } else {
            log.error("❌ Order {} not found in database!", event.orderId());
        }
    }
}
