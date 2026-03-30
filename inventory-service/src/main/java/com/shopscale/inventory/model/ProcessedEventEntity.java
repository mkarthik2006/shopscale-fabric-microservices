package com.shopscale.inventory.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {
    @Id
    private UUID eventId;
    private String status; // SUCCESS or FAILED_NO_STOCK

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(UUID eventId, String status) {
        this.eventId = eventId;
        this.status = status;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
