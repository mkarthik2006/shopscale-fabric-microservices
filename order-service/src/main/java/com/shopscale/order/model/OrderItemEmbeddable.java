package com.shopscale.order.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class OrderItemEmbeddable {
  private String sku;
  private Integer quantity;
  private BigDecimal unitPrice;

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}