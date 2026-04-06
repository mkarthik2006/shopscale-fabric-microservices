package com.shopscale.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency Guard (Project Doc Page 6 — "enable.idempotence: true").
 * Prevents duplicate notification emails for the same event.
 */
@Entity
@Table(name = "processed_notifications")
public class ProcessedNotificationEntity {

    @Id
    private UUID eventId;
    private String eventType;
    private Instant processedAt;

    public ProcessedNotificationEntity() {}

    public ProcessedNotificationEntity(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}