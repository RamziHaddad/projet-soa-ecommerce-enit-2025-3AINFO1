package com.recommendation.controller;

import com.recommendation.ml.ModelTrainerService;
import com.recommendation.ml.TrendingDetectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin Controller
 * Endpoints pour la gestion et le monitoring du système
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ModelTrainerService modelTrainer;
    private final TrendingDetectorService trendingDetector;

    /**
     * Déclenche l'entraînement des modèles manuellement
     */
    @PostMapping("/train")
    public ResponseEntity<Map<String, String>> triggerTraining() {
        log.info("Manual model training triggered via admin endpoint");
        
        try {
            // Lance l'entraînement en asynchrone
            new Thread(() -> {
                modelTrainer.triggerTraining();
            }).start();
            
            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "message", "Model training started in background"
            ));
        } catch (Exception e) {
            log.error("Failed to trigger training", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Récupère les produits en tendance
     */
    @GetMapping("/trending")
    public ResponseEntity<?> getTrending(
            @RequestParam(defaultValue = "4") int hours,
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            var trending = trendingDetector.getTrendingProducts(hours, limit);
            return ResponseEntity.ok(Map.of(
                    "trending", trending,
                    "count", trending.size(),
                    "hours", hours
            ));
        } catch (Exception e) {
            log.error("Failed to get trending products", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Récupère les produits en tendance depuis le cache
     */
    @GetMapping("/trending/cached")
    public ResponseEntity<?> getCachedTrending(@RequestParam(defaultValue = "20") int limit) {
        try {
            var trending = trendingDetector.getCachedTrendingProducts(limit);
            return ResponseEntity.ok(Map.of(
                    "trending", trending,
                    "count", trending.size()
            ));
        } catch (Exception e) {
            log.error("Failed to get cached trending", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Force la mise à jour du cache trending
     */
    @PostMapping("/trending/refresh")
    public ResponseEntity<Map<String, String>> refreshTrending() {
        try {
            trendingDetector.updateTrendingCache();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Trending cache refreshed"
            ));
        } catch (Exception e) {
            log.error("Failed to refresh trending cache", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check détaillé
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "services", Map.of(
                        "modelTrainer", "active",
                        "trendingDetector", "active",
                        "featureEngineer", "active"
                ),
                "timestamp", System.currentTimeMillis()
        ));
    }
}
