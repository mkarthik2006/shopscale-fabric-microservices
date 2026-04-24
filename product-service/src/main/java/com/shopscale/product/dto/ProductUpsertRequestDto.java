package com.shopscale.product.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ProductUpsertRequestDto {
    @NotBlank(message = "SKU is required")
    private String sku;
    @NotBlank(message = "Name is required")
    private String name;
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be >= 0")
    private BigDecimal price;
    @NotNull(message = "Stock is required")
    @PositiveOrZero(message = "Stock must be >= 0")
    private Integer stock;
    @NotNull(message = "Active flag is required")
    private Boolean active;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
