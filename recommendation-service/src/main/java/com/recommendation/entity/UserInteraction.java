package com.recommendation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité pour stocker les interactions utilisateur (clicks, vues, etc.)
 * Utilisé pour l'entraînement des modèles Word2Vec
 */
@Entity
@Table(name = "user_interactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInteraction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String productId;
    
    @Column(nullable = false)
    private String eventType; // click, view, add_to_cart, purchase
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private String sessionId;
    
    private String category;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
