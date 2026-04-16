package com.shopscale.inventory.repository;

import com.shopscale.inventory.model.InboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEventEntity, UUID> {
}
