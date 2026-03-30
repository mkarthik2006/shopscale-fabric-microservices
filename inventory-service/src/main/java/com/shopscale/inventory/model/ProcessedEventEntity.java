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

  public ProcessedEventEntity() {}
  public ProcessedEventEntity(UUID eventId) { this.eventId = eventId; }

  public UUID getEventId() { return eventId; }
  public void setEventId(UUID eventId) { this.eventId = eventId; }
}
