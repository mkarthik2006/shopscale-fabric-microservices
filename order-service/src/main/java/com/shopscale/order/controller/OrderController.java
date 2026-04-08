package com.shopscale.order.controller;

import com.shopscale.order.dto.OrderRequestDto;
import com.shopscale.order.dto.OrderResponseDto;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.model.OrderItemEmbeddable;
import com.shopscale.order.service.OrderService;
import com.shopscale.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService service;
    private final OrderRepository repository;
    
    public OrderController(OrderService service, OrderRepository repository) { 
        this.service = service; 
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody OrderRequestDto dto) {
        OrderEntity entity = new OrderEntity();
        entity.setUserId(dto.getUserId());
        entity.setCurrency(dto.getCurrency());
        entity.setTotalAmount(java.math.BigDecimal.valueOf(dto.getTotalAmount()));
        entity.setCreatedAt(Instant.now());
        
        List<OrderItemEmbeddable> items = dto.getItems().stream().map(i -> {
            OrderItemEmbeddable item = new OrderItemEmbeddable();
            item.setSku(i.getSku());
            item.setQuantity(i.getQuantity());
            item.setUnitPrice(java.math.BigDecimal.valueOf(i.getUnitPrice()));
            return item;
        }).collect(Collectors.toList());
        entity.setItems(items);

        OrderEntity saved = service.placeOrder(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new OrderResponseDto(saved.getId(), saved.getStatus(), saved.getUserId())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable UUID id) {
        OrderEntity saved = service.getById(id);
        return ResponseEntity.ok(new OrderResponseDto(saved.getId(), saved.getStatus(), saved.getUserId()));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getByUser(@RequestParam String userId) {
        // ✅ Utilizes the DTO Projection query
        List<OrderResponseDto> dtos = repository.findOrderSummariesByUser(userId);
        return ResponseEntity.ok(dtos);
    }
}
