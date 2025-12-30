package com.recommendation.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO pour les événements du service Catalog
 * Aligné avec le modèle Product du catalog-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogEventDTO {
    private Long id;
    private String productId;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String eventType; // CREATED, UPDATED, DELETED
}
