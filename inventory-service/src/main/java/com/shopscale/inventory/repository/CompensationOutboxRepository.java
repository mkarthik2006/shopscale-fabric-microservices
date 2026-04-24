package com.shopscale.inventory.repository;

import com.shopscale.inventory.model.CompensationOutboxEntity;
import com.shopscale.inventory.model.CompensationOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CompensationOutboxRepository extends JpaRepository<CompensationOutboxEntity, UUID> {
    List<CompensationOutboxEntity> findTop100ByStatusOrderByCreatedAtAsc(CompensationOutboxStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CompensationOutboxEntity e
            SET e.status = :newStatus
            WHERE e.id = :id AND e.status = :expectedStatus
            """)
    int updateStatusIfCurrent(@Param("id") UUID id,
                              @Param("expectedStatus") CompensationOutboxStatus expectedStatus,
                              @Param("newStatus") CompensationOutboxStatus newStatus);
}
