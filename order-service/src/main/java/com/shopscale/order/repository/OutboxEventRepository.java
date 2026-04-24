package com.shopscale.order.repository;

import com.shopscale.order.model.OutboxEventEntity;
import com.shopscale.order.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.status = :newStatus
            WHERE e.id = :id AND e.status = :expectedStatus
            """)
    int updateStatusIfCurrent(@Param("id") UUID id,
                              @Param("expectedStatus") OutboxStatus expectedStatus,
                              @Param("newStatus") OutboxStatus newStatus);
}
