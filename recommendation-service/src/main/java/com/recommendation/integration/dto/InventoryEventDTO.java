package com.recommendation.integration.dto;

import lombok.Data;

/**
 * DTO pour les événements d'inventaire
 */
@Data
public class InventoryEventDTO {
    private String productId;
    private Integer stock;
    private Integer reserved;
    private String eventType; // STOCK_UPDATED, LOW_STOCK, OUT_OF_STOCK
    private String timestamp;
}
