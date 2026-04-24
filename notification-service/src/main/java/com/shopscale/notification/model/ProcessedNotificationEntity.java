package com.shopscale.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency Guard (Project Doc Page 6 — "enable.idempotence: true").
 * Prevents duplicate notification emails for the same event.
 */
@Entity
@Table(name = "processed_notifications")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "notification.ProcessedNotificationEntity")
public class ProcessedNotificationEntity {

    @Id
    private UUID eventId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String status = "PROCESSED";
    @Column(nullable = false)
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}