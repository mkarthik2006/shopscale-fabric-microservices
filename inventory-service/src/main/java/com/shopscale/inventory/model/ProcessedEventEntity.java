package com.shopscale.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    // Canonical values enforced by DB constraints: PROCESSED, FAILED, RETRYING.
    private String status;
    @Column(nullable = false)
    private Instant processedAt;

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(UUID eventId, String status) {
        this.eventId = eventId;
        this.status = status;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
