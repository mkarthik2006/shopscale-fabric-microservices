package com.shopscale.inventory.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "inventory")
@Check(constraints = "stock >= 0")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "inventory.InventoryEntity")
public class InventoryEntity {
  @Id
  @Column(nullable = false, length = 64)
  private String sku;
  @Column(nullable = false)
  private Integer stock;




  @Version
  private Long version;

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
