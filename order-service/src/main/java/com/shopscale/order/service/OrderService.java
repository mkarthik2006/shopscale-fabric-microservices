package com.shopscale.order.service;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
  private final OrderRepository repository;
  private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

  public OrderService(OrderRepository repository, KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
    this.repository = repository;
    this.kafkaTemplate = kafkaTemplate;
  }

  @Transactional
  public OrderEntity placeOrder(OrderEntity order) {
    order.setStatus("PLACED");
    OrderEntity saved = repository.save(order);

    List<OrderPlacedEvent.Item> eventItems = saved.getItems().stream()
        .map(i -> new OrderPlacedEvent.Item(i.getSku(), i.getQuantity(), i.getUnitPrice()))
        .toList();

    OrderPlacedEvent event = new OrderPlacedEvent(
        UUID.randomUUID(),
        "ORDER_PLACED",
        saved.getCreatedAt(),
        saved.getId(),
        saved.getUserId(),
        eventItems,
        saved.getTotalAmount(),
        saved.getCurrency()
    );

    kafkaTemplate.send("order.placed", saved.getId().toString(), event);
    return saved;
  }

  public OrderEntity getById(UUID id) { return repository.findById(id).orElseThrow(); }
  public List<OrderEntity> byUser(String userId) { return repository.findByUserId(userId); }
}