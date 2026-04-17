package com.shopscale.inventory.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "inventory")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "inventory.InventoryEntity")
public class InventoryEntity {
  @Id
  private String sku;
  private Integer stock;

  // Optimistic locking — prevents lost updates when concurrent OrderPlaced
  // events for the same SKU are processed in parallel by multi-partition
  // Kafka listeners (PROJECT_RULES.md §1 concurrency correctness).
  @Version
  private Long version;

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
