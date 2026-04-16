package com.shopscale.product.dto;

import java.math.BigDecimal;

public class ProductDTO {

    private String id;
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Boolean active;

    public ProductDTO(String id, String sku, String name,
                      BigDecimal price, Integer stock, Boolean active) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.active = active;
    }

    public String getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public Boolean getActive() { return active; }
}