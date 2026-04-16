package com.shopscale.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_event")
public class InboxEventEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InboxEventStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    public InboxEventEntity() {
    }

    public InboxEventEntity(UUID eventId, String eventType, InboxEventStatus status) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public InboxEventStatus getStatus() {
        return status;
    }

    public void setStatus(InboxEventStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
