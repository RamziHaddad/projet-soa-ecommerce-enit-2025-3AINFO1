package com.recommendation.ml;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Trending Detector Service
 * Détecte les produits en tendance en temps réel
 * Utilise Redis pour stocker les compteurs de vues/achats par fenêtre temporelle
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingDetectorService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TREND_KEY_PREFIX = "trending:";
    private static final String HOURLY_VIEWS_PREFIX = "hourly_views:";
    private static final String HOURLY_PURCHASES_PREFIX = "hourly_purchases:";
    private static final int TRENDING_THRESHOLD = 10; // Minimum events to be considered trending
    private static final int TOP_TRENDING_LIMIT = 50;

    /**
     * Enregistre une vue de produit pour le trending
     */
    public void recordProductView(String productId) {
        String hour = getCurrentHourKey();
        String key = HOURLY_VIEWS_PREFIX + hour;
        
        try {
            redisTemplate.opsForZSet().incrementScore(key, productId, 1.0);
            // Expire after 24 hours
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to record product view: {}", productId, e);
        }
    }

    /**
     * Enregistre un événement générique (view, purchase, click, etc)
     * Appelé par les Kafka listeners
     */
    public void recordEvent(String productId, String eventType, Integer quantity) {
        switch (eventType.toLowerCase()) {
            case "view":
            case "click":
                recordProductView(productId);
                break;
            case "purchase":
                // Enregistrer chaque achat selon la quantité
                for (int i = 0; i < quantity; i++) {
                    recordProductPurchase(productId);
                }
                break;
            case "add_to_cart":
                // Poids réduit pour les panier (0.5x)
                String hour = getCurrentHourKey();
                String key = HOURLY_VIEWS_PREFIX + hour;
                try {
                    redisTemplate.opsForZSet().incrementScore(key, productId, 0.5);
                    redisTemplate.expire(key, 24, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.error("Failed to record add_to_cart: {}", productId, e);
                }
                break;
            default:
                log.warn("Unknown event type: {}", eventType);
        }
    }

    /**
     * Enregistre un achat de produit pour le trending
     */
    public void recordProductPurchase(String productId) {
        String hour = getCurrentHourKey();
        String key = HOURLY_PURCHASES_PREFIX + hour;
        
        try {
            // Les achats comptent 5x plus que les vues
            redisTemplate.opsForZSet().incrementScore(key, productId, 5.0);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to record product purchase: {}", productId, e);
        }
    }

    /**
     * Récupère les produits en tendance pour une période donnée
     */
    public List<TrendingProduct> getTrendingProducts(int hours, int limit) {
        String currentHour = getCurrentHourKey();
        List<String> hourKeys = getLastNHours(hours);
        
        Map<String, Double> productScores = new HashMap<>();
        
        for (String hourKey : hourKeys) {
            // Aggregate views
            String viewKey = HOURLY_VIEWS_PREFIX + hourKey;
            var viewScores = redisTemplate.opsForZSet().reverseRangeWithScores(viewKey, 0, -1);
            if (viewScores != null) {
                viewScores.forEach(item -> {
                    String productId = item.getValue();
                    Double score = item.getScore();
                    productScores.merge(productId, score, Double::sum);
                });
            }
            
            // Aggregate purchases
            String purchaseKey = HOURLY_PURCHASES_PREFIX + hourKey;
            var purchaseScores = redisTemplate.opsForZSet().reverseRangeWithScores(purchaseKey, 0, -1);
            if (purchaseScores != null) {
                purchaseScores.forEach(item -> {
                    String productId = item.getValue();
                    Double score = item.getScore();
                    productScores.merge(productId, score, Double::sum);
                });
            }
        }
        
        // Calculate trending score with time decay
        return productScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= TRENDING_THRESHOLD)
                .map(entry -> {
                    String productId = entry.getKey();
                    double rawScore = entry.getValue();
                    // Apply time decay: recent hours get higher weight
                    double trendingScore = calculateTrendingScore(productId, hourKeys, rawScore);
                    return new TrendingProduct(productId, trendingScore, (int) rawScore);
                })
                .sorted(Comparator.comparingDouble(TrendingProduct::trendingScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calcule le score de tendance avec décroissance temporelle
     */
    private double calculateTrendingScore(String productId, List<String> hourKeys, double rawScore) {
        double weightedScore = 0.0;
        int totalHours = hourKeys.size();
        
        for (int i = 0; i < hourKeys.size(); i++) {
            String hourKey = hourKeys.get(i);
            
            // Weight: recent hours have higher weight
            double hourWeight = 1.0 - (i * 0.1); // Decay by 10% per hour
            hourWeight = Math.max(0.1, hourWeight); // Minimum 10% weight
            
            // Get score for this hour
            String viewKey = HOURLY_VIEWS_PREFIX + hourKey;
            Double viewScore = redisTemplate.opsForZSet().score(viewKey, productId);
            
            String purchaseKey = HOURLY_PURCHASES_PREFIX + hourKey;
            Double purchaseScore = redisTemplate.opsForZSet().score(purchaseKey, productId);
            
            double hourScore = (viewScore != null ? viewScore : 0.0) + 
                             (purchaseScore != null ? purchaseScore : 0.0);
            
            weightedScore += hourScore * hourWeight;
        }
        
        return weightedScore;
    }

    /**
     * Récupère les top trending products et les cache
     * Scheduled: toutes les 15 minutes
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15 minutes
    public void updateTrendingCache() {
        log.info("Updating trending products cache...");
        
        try {
            // Get trending for last 4 hours
            List<TrendingProduct> trending = getTrendingProducts(4, TOP_TRENDING_LIMIT);
            
            // Cache the results
            String cacheKey = TREND_KEY_PREFIX + "top_4h";
            redisTemplate.delete(cacheKey);
            
            for (int i = 0; i < trending.size(); i++) {
                TrendingProduct product = trending.get(i);
                redisTemplate.opsForZSet().add(cacheKey, product.productId(), product.trendingScore());
            }
            
            redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
            
            log.info("Updated trending cache with {} products", trending.size());
            
        } catch (Exception e) {
            log.error("Failed to update trending cache", e);
        }
    }

    /**
     * Récupère les produits en tendance depuis le cache
     */
    public List<String> getCachedTrendingProducts(int limit) {
        String cacheKey = TREND_KEY_PREFIX + "top_4h";
        
        try {
            var trending = redisTemplate.opsForZSet().reverseRange(cacheKey, 0, limit - 1);
            return trending != null ? new ArrayList<>(trending) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to get cached trending products", e);
            return Collections.emptyList();
        }
    }

    /**
     * Vérifie si un produit est en tendance
     */
    public boolean isTrending(String productId) {
        String cacheKey = TREND_KEY_PREFIX + "top_4h";
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(cacheKey, productId);
            return rank != null && rank < TOP_TRENDING_LIMIT;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère le boost score si le produit est trending
     */
    public double getTrendingBoost(String productId) {
        if (!isTrending(productId)) {
            return 0.0;
        }
        
        String cacheKey = TREND_KEY_PREFIX + "top_4h";
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(cacheKey, productId);
            if (rank != null) {
                // Top 10: +0.3 boost, 11-30: +0.2, 31-50: +0.1
                if (rank < 10) return 0.3;
                if (rank < 30) return 0.2;
                return 0.1;
            }
        } catch (Exception e) {
            log.error("Failed to get trending boost", e);
        }
        return 0.0;
    }

    private String getCurrentHourKey() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d-%02d-%02d-%02d", 
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }

    private List<String> getLastNHours(int n) {
        List<String> hours = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < n; i++) {
            LocalDateTime hour = now.minus(i, ChronoUnit.HOURS);
            hours.add(String.format("%04d-%02d-%02d-%02d", 
                    hour.getYear(), hour.getMonthValue(), hour.getDayOfMonth(), hour.getHour()));
        }
        
        return hours;
    }

    /**
     * Produit en tendance
     */
    public record TrendingProduct(
            String productId,
            double trendingScore,
            int eventCount
    ) {}
}
