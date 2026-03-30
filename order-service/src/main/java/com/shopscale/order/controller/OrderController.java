package com.shopscale.order.controller;

import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService service;
  public OrderController(OrderService service) { this.service = service; }

  @PostMapping
  public OrderEntity place(@RequestBody OrderEntity order) { return service.placeOrder(order); }

  @GetMapping("/{id}")
  public OrderEntity one(@PathVariable UUID id) { return service.getById(id); }

  @GetMapping
  public List<OrderEntity> byUser(@RequestParam String userId) { return service.byUser(userId); }
}