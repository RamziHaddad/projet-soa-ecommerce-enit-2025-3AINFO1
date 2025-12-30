package com.recommendation.ml;

import com.recommendation.repository.ProductRepository;
import com.recommendation.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature Engineer
 * Extrait les features contextuelles et comportementales pour le ML ranking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureEngineer {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ProductRepository productRepository;
    private final TrendingDetectorService trendingDetectorService;
    private final Map<String, Map<String, Object>> productFeatureCache = new ConcurrentHashMap();

    /**
     * Extrait toutes les features pour un couple (user, product, context)
     */
    public Features extractFeatures(String userId, String productId, Map<String, Object> context) {
        Map<String, Double> features = new HashMap<>();

        // Temporal features
        features.put("hour_of_day", (double) LocalTime.now().getHour());
        features.put("day_of_week", (double) LocalDate.now().getDayOfWeek().getValue());
        features.put("is_weekend", LocalDate.now().getDayOfWeek().getValue() >= 6 ? 1.0 : 0.0);

        // User behavioral features
        features.put("user_activity_level", getUserActivityLevel(userId));
        features.put("user_avg_session_duration", getUserAvgSessionDuration(userId));
        features.put("user_purchase_frequency", getUserPurchaseFrequency(userId));
        features.put("user_avg_order_value", getUserAvgOrderValue(userId));
        features.put("user_account_age_days", getUserAccountAgeDays(userId));

        // Product features
        features.put("product_popularity", getProductPopularity(productId));
        features.put("product_avg_rating", getProductAvgRating(productId));
        features.put("product_price", getProductPrice(productId));
        features.put("product_in_stock", isProductInStock(productId) ? 1.0 : 0.0);
        features.put("product_available_stock", getProductAvailableStock(productId));
        features.put("product_age_days", getProductAgeDays(productId));
        features.put("trending_score", getTrendingScore(productId));

        // User-Product interaction features
        features.put("user_viewed_product_before", hasUserViewedProduct(userId, productId) ? 1.0 : 0.0);
        features.put("user_viewed_category_before", hasUserViewedCategory(userId, getProductCategory(productId)) ? 1.0 : 0.0);
        features.put("user_product_view_count", getUserProductViewCount(userId, productId));

        // Contextual features from request
        if (context != null) {
            features.put("is_experiment", context.getOrDefault("isExperiment", false).equals(true) ? 1.0 : 0.0);

            String season = (String) context.getOrDefault("season", "");
            features.put("is_ramadan", season.contains("ramadan") ? 1.0 : 0.0);
            features.put("is_winter", season.contains("winter") ? 1.0 : 0.0);
            features.put("is_summer", season.contains("summer") ? 1.0 : 0.0);
        }

        // Cross features
        features.put("price_user_budget_ratio", getPriceUserBudgetRatio(userId, productId));
        features.put("category_match_score", getCategoryMatchScore(userId, productId));

        return new Features(features);
    }

    public void updateProductStock(String productId, int stock) {
        productFeatureCache.computeIfAbsent(productId, k -> new HashMap<>()).put("available_stock", stock);
    }

    private boolean isProductInStock(String productId) {
        return getProductAvailableStock(productId) > 0;
    }

    private double getProductAvailableStock(String productId) {
        return (int) productFeatureCache.getOrDefault(productId, new HashMap<>()).getOrDefault("available_stock", 0.0);
    }

    // User behavioral features

    private double getUserActivityLevel(String userId) {
        try {
            long eventCount = userBehaviorRepository.countEventsByUserId(userId);
            // Normalize: 0-1 based on log scale
            return Math.min(1.0, Math.log10(eventCount + 1) / 3.0);
        } catch (Exception e) {
            return 0.5; // Default medium activity
        }
    }

    private double getUserAvgSessionDuration(String userId) {
        try {
            return userBehaviorRepository.getAvgSessionDuration(userId);
        } catch (Exception e) {
            return 300.0; // Default 5 minutes
        }
    }

    private double getUserPurchaseFrequency(String userId) {
        try {
            long purchases = userBehaviorRepository.countPurchasesByUserId(userId);
            long days = (long) getUserAccountAgeDays(userId);
            return days > 0 ? (double) purchases / days : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getUserAvgOrderValue(String userId) {
        try {
            return userBehaviorRepository.getAvgOrderValue(userId);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getUserAccountAgeDays(String userId) {
        try {
            return userBehaviorRepository.getAccountAgeDays(userId);
        } catch (Exception e) {
            return 1.0;
        }
    }

    // Product features

    private double getProductPopularity(String productId) {
        try {
            long viewCount = productRepository.getViewCount(productId);
            return Math.min(1.0, Math.log10(viewCount + 1) / 4.0);
        } catch (Exception e) {
            return 0.5;
        }
    }

    private double getProductAvgRating(String productId) {
        try {
            return productRepository.getAvgRating(productId);
        } catch (Exception e) {
            return 3.0; // Default rating
        }
    }

    private double getProductPrice(String productId) {
        try {
            return productRepository.getPrice(productId);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getProductStockLevel(String productId) {
        try {
            return productRepository.getStockLevel(productId);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private double getProductAgeDays(String productId) {
        try {
            return productRepository.getProductAgeDays(productId);
        } catch (Exception e) {
            return 30.0;
        }
    }

    private String getProductCategory(String productId) {
        try {
            return productRepository.getCategory(productId);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // User-Product interaction features

    private boolean hasUserViewedProduct(String userId, String productId) {
        try {
            return userBehaviorRepository.hasUserViewedProduct(userId, productId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasUserViewedCategory(String userId, String category) {
        try {
            return userBehaviorRepository.hasUserViewedCategory(userId, category);
        } catch (Exception e) {
            return false;
        }
    }

    private double getUserProductViewCount(String userId, String productId) {
        try {
            return userBehaviorRepository.getUserProductViewCount(userId, productId);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Cross features

    private double getPriceUserBudgetRatio(String userId, String productId) {
        try {
            double price = getProductPrice(productId);
            double budget = userBehaviorRepository.getUserBudget(userId);
            return budget > 0 ? price / budget : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getCategoryMatchScore(String userId, String productId) {
        try {
            String productCategory = getProductCategory(productId);
            var userTopCategories = userBehaviorRepository.getUserTopCategories(userId, 5);
            
            for (int i = 0; i < userTopCategories.size(); i++) {
                if (userTopCategories.get(i).equals(productCategory)) {
                    // Higher score for top-ranked categories
                    return 1.0 - (i * 0.15);
                }
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Enregistre l'achat d'un produit (pour les co-occurrences et User2Vec)
     */
    public void recordPurchase(String userId, String productId, Integer quantity) {
        try {
            // Log le signal d'achat (sera utilisé par le training batch)
            log.debug("Purchase recorded: user={}, product={}, qty={}", userId, productId, quantity);
            // Note: Les achats sont enregistrés via Kafka OrderEventListener -> DB
            // Le training périodique (ModelTrainerService) utilisera ces données
        } catch (Exception e) {
            log.error("Error recording purchase", e);
        }
    }

    /**
     * Extrait les features d'un produit (appelé lors des événements catalog)
     */
    public void extractProductFeatures(String productId, String name, String description, double price) {
        try {
            // Mettre en cache les features du produit
            Map<String, Object> pf = productFeatureCache.computeIfAbsent(productId, k -> new HashMap<>());
            pf.put("name", name);
            pf.put("description", description);
            pf.put("price", price);
            pf.put("updated_at", System.currentTimeMillis());
            
            // Extraire les tags/catégories de la description
            String[] keywords = description != null ? description.split("\\s+") : new String[0];
            pf.put("keywords_count", (double) keywords.length);
            
            log.debug("Product features extracted: product={}, price={}", productId, price);
        } catch (Exception e) {
            log.error("Error extracting product features", e);
        }
    }

    /**
     * Enregistre un feedback utilisateur (rating/review)
     */
    public void recordFeedback(String userId, String productId, Integer rating, String comment) {
        try {
            // Log le signal de feedback
            log.debug("Feedback recorded: user={}, product={}, rating={}", userId, productId, rating);
            // Note: Les feedbacks sont stockés dans la DB via les listeners Kafka
            // Le training utilisera ces données pour ajuster les scores
        } catch (Exception e) {
            log.error("Error recording feedback", e);
        }
    }
    
    /**
     * Met à jour le stock d'un produit (depuis Inventory Service)
     */
    public void updateProductStock(String productId, Integer stock, Integer reserved) {
        try {
            // Cache du stock pour éviter de recommander des produits épuisés
            Map<String, Object> pf = productFeatureCache.computeIfAbsent(productId, k -> new HashMap<>());
            double s = stock != null ? stock.doubleValue() : 0.0;
            double r = reserved != null ? reserved.doubleValue() : 0.0;
            double available = s - r;
            pf.put("stock", s);
            pf.put("reserved", r);
            pf.put("available", available);
            pf.put("is_in_stock", available > 0 ? 1.0 : 0.0);
            
            log.debug("Product stock updated: product={}, stock={}, available={}", 
                productId, stock, available);
        } catch (Exception e) {
            log.error("Error updating product stock", e);
        }
    }

    private boolean isProductInStock(String productId) {
        Map<String, Object> pf = productFeatureCache.get(productId);
        if (pf != null) {
            Object inStock = pf.get("is_in_stock");
            if (inStock instanceof Number) {
                return ((Number) inStock).doubleValue() > 0.0;
            }
        }
        return getProductStockLevel(productId) > 0.0;
    }

    private double getProductAvailableStock(String productId) {
        Map<String, Object> pf = productFeatureCache.get(productId);
        if (pf != null) {
            Object available = pf.get("available");
            if (available instanceof Number) {
                return ((Number) available).doubleValue();
            }
        }
        return 0.0;
    }

    private double getTrendingScore(String productId) {
        try {
            double boost = trendingDetectorService.getTrendingBoost(productId);
            return Math.max(0.0, Math.min(1.0, boost));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Container for extracted features
     */
    public record Features(Map<String, Double> features) {
        
        public double get(String featureName) {
            return features.getOrDefault(featureName, 0.0);
        }
        
        public double[] toArray() {
            return features.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();
        }
        
        public int size() {
            return features.size();
        }
    }
}
