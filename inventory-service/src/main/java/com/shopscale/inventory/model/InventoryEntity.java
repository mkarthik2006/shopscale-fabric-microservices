package com.shopscale.inventory.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "inventory")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "inventory.InventoryEntity")
public class InventoryEntity {
  @Id
  private String sku;
  private Integer stock;

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
}
