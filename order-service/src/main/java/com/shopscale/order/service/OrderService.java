package com.shopscale.order.service;

import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.dto.OrderResponseDto;
import com.shopscale.order.repository.OrderRepository;
import com.shopscale.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderOutboxMapper outboxMapper;

    public OrderService(OrderRepository repository, 
                        OutboxEventRepository outboxEventRepository,
                        OrderOutboxMapper outboxMapper) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxMapper = outboxMapper;
    }

    @Transactional
    public OrderEntity placeOrder(OrderEntity order) {
        order.setStatus("PLACED");
        OrderEntity saved = repository.save(order);
        String payload = outboxMapper.toPayload(saved);

        OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId(saved.getId());
        outboxEvent.setEventType("ORDER_PLACED");
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(outboxEvent);

        log.info("Order and outbox event persisted transactionally | orderId={}", saved.getId());
        return saved;
    }
    
    public OrderEntity getById(UUID id) { return repository.findById(id).orElseThrow(); }
    public List<OrderEntity> byUser(String userId) { return repository.findByUserId(userId); }
    public List<OrderResponseDto> byUserSummaries(String userId) { return repository.findOrderSummariesByUser(userId); }
}
