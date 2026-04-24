package com.shopscale.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compensation_outbox")
public class CompensationOutboxEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID sourceEventId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompensationOutboxStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    @PrePersist
    void onPersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = CompensationOutboxStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CompensationOutboxStatus getStatus() {
        return status;
    }

    public void setStatus(CompensationOutboxStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
