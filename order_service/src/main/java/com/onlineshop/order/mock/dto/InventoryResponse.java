package com.onlineshop.order.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de réponse du service Inventory
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    
    private boolean success;
    private String message;
    private List<InventoryItem> items;
    private BigDecimal totalValue;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private Long productId;
        private String productName;
        private Integer availableQuantity;
        private Integer reservedQuantity;
        private BigDecimal unitPrice;
        private boolean available;
    }
}
