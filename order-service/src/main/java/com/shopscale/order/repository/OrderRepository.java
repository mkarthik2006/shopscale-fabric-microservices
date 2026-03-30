package com.shopscale.order.repository;

import com.shopscale.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
  List<OrderEntity> findByUserId(String userId);
}