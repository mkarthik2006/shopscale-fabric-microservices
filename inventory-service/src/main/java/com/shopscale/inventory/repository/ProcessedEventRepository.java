package com.shopscale.inventory.repository;

import com.shopscale.inventory.model.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {}
