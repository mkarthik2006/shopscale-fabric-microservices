package com.shopscale.order.repository;

import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.dto.OrderResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    
  List<OrderEntity> findByUserId(String userId);


  @Query("SELECT new com.shopscale.order.dto.OrderResponseDto(o.id, o.status, o.userId) FROM OrderEntity o WHERE o.userId = :userId")
  List<OrderResponseDto> findOrderSummariesByUser(@Param("userId") String userId);
}
