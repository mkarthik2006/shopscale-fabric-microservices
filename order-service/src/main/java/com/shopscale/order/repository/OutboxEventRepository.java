package com.shopscale.order.repository;

import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
