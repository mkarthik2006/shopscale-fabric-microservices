package com.shopscale.order.service;

import com.shopscale.order.dto.OrderRequestDto;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import com.shopscale.order.dto.OrderResponseDto;
import com.shopscale.common.exception.BusinessException;
import com.shopscale.order.repository.OrderRepository;
import com.shopscale.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderOutboxMapper outboxMapper;
    private final PriceClientService priceClientService;

    public OrderService(OrderRepository repository, 
                        OutboxEventRepository outboxEventRepository,
                        OrderOutboxMapper outboxMapper,
                        PriceClientService priceClientService) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxMapper = outboxMapper;
        this.priceClientService = priceClientService;
    }

    @Transactional
    public OrderEntity placeOrder(OrderEntity order) {
        if (order.getUserEmail() == null || order.getUserEmail().isBlank() || !order.getUserEmail().contains("@")) {
            throw new BusinessException("Order requires a valid user email");
        }
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

    public OrderResponseDto placeOrder(OrderRequestDto dto) {
        List<OrderItemEmbeddable> items = resolveItemsWithAuthoritativePrices(dto);
        return placeOrderTransactional(dto, items);
    }

    @Transactional
    protected OrderResponseDto placeOrderTransactional(OrderRequestDto dto, List<OrderItemEmbeddable> items) {
        OrderEntity entity = new OrderEntity();
        entity.setUserId(dto.getUserId());
        entity.setUserEmail(dto.getUserEmail());
        entity.setCurrency(dto.getCurrency());
        entity.setCreatedAt(Instant.now());
        entity.setItems(items);
        entity.setTotalAmount(calculateTotal(items));

        OrderEntity saved = placeOrder(entity);
        return new OrderResponseDto(saved.getId(), saved.getStatus(), saved.getUserId());
    }
    
    public OrderEntity getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Order not found with id: " + id));
    }
    public List<OrderEntity> byUser(String userId) { return repository.findByUserId(userId); }
    public List<OrderResponseDto> byUserSummaries(String userId) { return repository.findOrderSummariesByUser(userId); }

    private List<OrderItemEmbeddable> resolveItemsWithAuthoritativePrices(OrderRequestDto dto) {
        return dto.getItems().stream().map(i -> {
            OrderItemEmbeddable item = new OrderItemEmbeddable();
            item.setSku(i.getSku());
            item.setQuantity(i.getQuantity());
            item.setUnitPrice(priceClientService.resolveUnitPrice(i.getSku()));
            return item;
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(List<OrderItemEmbeddable> items) {
        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Order total must be positive");
        }
        return total;
    }
}
