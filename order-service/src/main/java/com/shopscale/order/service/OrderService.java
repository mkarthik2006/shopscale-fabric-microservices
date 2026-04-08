package com.shopscale.order.service;

import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.repository.OrderRepository;
import com.shopscale.common.events.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final ExecutorService executorService;

    @Value("${app.kafka.topic.order-placed}")
    private String topic;

    public OrderService(OrderRepository repository, 
                        KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate,
                        ExecutorService executorService) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.executorService = executorService;
    }

    @Transactional
    public OrderEntity placeOrder(OrderEntity order) {
        order.setStatus("PLACED");
        OrderEntity saved = repository.save(order);

        // Required Usage of Virtual Threads
        executorService.submit(() -> processOrderAsync(saved));

        return saved;
    }

    private void processOrderAsync(OrderEntity saved) {
        List<OrderPlacedEvent.Item> eventItems = saved.getItems().stream()
                .map(i -> new OrderPlacedEvent.Item(i.getSku(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(), "ORDER_PLACED", saved.getCreatedAt(),
                saved.getId(), saved.getUserId(), eventItems,
                saved.getTotalAmount(), saved.getCurrency()
        );

        log.info("Sending OrderPlacedEvent to Kafka via Virtual Thread | orderId={}", saved.getId());
        kafkaTemplate.send(topic, saved.getId().toString(), event);
    }
    
    public OrderEntity getById(UUID id) { return repository.findById(id).orElseThrow(); }
    public List<OrderEntity> byUser(String userId) { return repository.findByUserId(userId); }
}
