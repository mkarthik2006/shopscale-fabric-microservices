package com.shopscale.order.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import java.math.BigDecimal;

@Embeddable
public class OrderItemEmbeddable {
  @Column(nullable = false, length = 255)
  private String sku;
  @Column(nullable = false)
  private Integer quantity;
  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPrice;

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}