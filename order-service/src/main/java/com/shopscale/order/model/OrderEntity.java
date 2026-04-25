package com.shopscale.order.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id_created_at", columnList = "userId, createdAt"),
        @Index(name = "idx_orders_status_created_at", columnList = "status, createdAt")
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "order.OrderEntity")
public class OrderEntity {
  @Id
  private UUID id;
  @Column(nullable = false, length = 64)
  private String userId;
  @Column(nullable = false, length = 254)
  private String userEmail;
  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;
  @Column(nullable = false, length = 3)
  private String currency;
  @Column(nullable = false, length = 32)
  private String status;
  @Column(nullable = false)
  private Instant createdAt;

  
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "order_items",
      joinColumns = @JoinColumn(name = "order_id"),
      indexes = @Index(name = "idx_order_items_order_id", columnList = "order_id")
  )
  @BatchSize(size = 50)
  private List<OrderItemEmbeddable> items = new ArrayList<>();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }


  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getUserEmail() { return userEmail; }
  public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public List<OrderItemEmbeddable> getItems() { return items; }
  public void setItems(List<OrderItemEmbeddable> items) { this.items = items; }
}
