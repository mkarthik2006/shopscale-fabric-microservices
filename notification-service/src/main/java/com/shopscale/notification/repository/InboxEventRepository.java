package com.shopscale.notification.repository;

import com.shopscale.notification.model.InboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEventEntity, UUID> {
}
