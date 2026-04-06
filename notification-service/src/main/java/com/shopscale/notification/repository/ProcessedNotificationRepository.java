package com.shopscale.notification.repository;

import com.shopscale.notification.model.ProcessedNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedNotificationRepository extends JpaRepository<ProcessedNotificationEntity, UUID> {}