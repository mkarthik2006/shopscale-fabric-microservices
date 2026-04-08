package com.shopscale.order.dto;
import java.util.List;
import lombok.Data;
@Data
public class OrderRequestDto {
    private String userId;
    private String currency;
    private Double totalAmount;
    private List<ItemDto> items;
    @Data
    public static class ItemDto {
        private String sku;
        private Integer quantity;
        private Double unitPrice;
    }
}