package com.shopscale.product.model;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
public class Product {
  @Id
  private String id;
  private String sku;
  private String name;
  private BigDecimal price;
  private Integer stock;
  private Boolean active;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }
}