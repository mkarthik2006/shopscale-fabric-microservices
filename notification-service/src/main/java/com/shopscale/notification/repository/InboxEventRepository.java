package com.shopscale.notification.repository;

import com.shopscale.notification.model.InboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEventEntity, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InboxEventEntity e
            SET e.status = :newStatus
            WHERE e.eventId = :id AND e.status = :expectedStatus
            """)
    int updateStatusIfCurrent(@Param("id") UUID id,
                              @Param("expectedStatus") com.shopscale.notification.model.InboxEventStatus expectedStatus,
                              @Param("newStatus") com.shopscale.notification.model.InboxEventStatus newStatus);
}
