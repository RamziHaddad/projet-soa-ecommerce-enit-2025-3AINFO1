package com.ecommerce.recommendation.service;

import com.ecommerce.recommendation.model.Product;
import com.ecommerce.recommendation.model.RecommendationRequest;
import com.ecommerce.recommendation.model.RecommendationResponse;
import com.ecommerce.recommendation.model.ScoredProduct;
import com.ecommerce.recommendation.repository.ProductRepository;
import com.ecommerce.recommendation.repository.UserBehaviorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserBehaviorRepository userBehaviorRepository;

    // Scoring weights
    private static final double POPULARITY_WEIGHT = 0.20;
    private static final double RATING_WEIGHT = 0.25;
    private static final double RECENCY_WEIGHT = 0.15;
    private static final double CATEGORY_AFFINITY_WEIGHT = 0.20;
    private static final double COLLABORATIVE_WEIGHT = 0.20;

    /**
     * Main method to get recommendations
     */
    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        String userId = request.getUserId();
        int limit = request.getLimit() != null ? request.getLimit() : 10;
        String categoryFilter = request.getCategoryFilter();

        // Step 1: Fetch products
        List<Product> products = categoryFilter != null 
            ? productRepository.findByCategory(categoryFilter)
            : productRepository.findAllActive();

        // Step 2: Calculate scores for each product
        List<ScoredProduct> scoredProducts = products.stream()
            .map(product -> calculateProductScore(product, userId))
            .sorted(Comparator.comparingDouble(ScoredProduct::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        // Step 3: Build response
        return RecommendationResponse.builder()
            .userId(userId)
            .recommendations(scoredProducts)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Calculate comprehensive score for a product
     * This is the MAIN scoring algorithm
     */
    private ScoredProduct calculateProductScore(Product product, String userId) {
        // Calculate all 5 component scores
        double popularityScore = calculatePopularityScore(product);
        double ratingScore = calculateRatingScore(product);
        double recencyScore = calculateRecencyScore(product);
        double categoryAffinityScore = calculateCategoryAffinityScore(product, userId);
        double collaborativeScore = calculateCollaborativeScore(product, userId);

        // Calculate weighted final score
        double finalScore = 
            (popularityScore * POPULARITY_WEIGHT) +
            (ratingScore * RATING_WEIGHT) +
            (recencyScore * RECENCY_WEIGHT) +
            (categoryAffinityScore * CATEGORY_AFFINITY_WEIGHT) +
            (collaborativeScore * COLLABORATIVE_WEIGHT);

        // Build result
        return ScoredProduct.builder()
            .productId(product.getId())
            .productName(product.getName())
            .category(product.getCategory())
            .price(product.getPrice())
            .imageUrl(product.getImageUrl())
            .score(finalScore)
            .scoreBreakdown(Map.of(
                "popularity", popularityScore,
                "rating", ratingScore,
                "recency", recencyScore,
                "categoryAffinity", categoryAffinityScore,
                "collaborative", collaborativeScore
            ))
            .build();
    }

    /**
     * SCORE 1: Popularity
     * Returns: 0-100
     */
    private double calculatePopularityScore(Product product) {
        long views = product.getViewCount() != null ? product.getViewCount() : 0;
        long sales = product.getSalesCount() != null ? product.getSalesCount() : 0;
        
        // Sales are 5x more important than views
        double rawScore = (sales * 5) + views;
        
        // Use log scale to handle large numbers
        // Products with 1000 sales + 5000 views = score ~87.5
        return Math.min(100, Math.log1p(rawScore) * 10);
    }

    /**
     * SCORE 2: Rating (based on customer reviews)
     * Returns: 0-100
     */
    private double calculateRatingScore(Product product) {
        double rating = product.getAverageRating() != null ? product.getAverageRating() : 0;
        int reviewCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
        
        // Confidence factor: products with more reviews get full rating
        // Products with < 10 reviews get penalized
        double confidence = Math.min(1.0, reviewCount / 10.0);
        
        // Convert 5-star rating to 0-100 scale
        // Example: 4.5★ with 15 reviews = (4.5/5) * 100 * 1.0 = 90.0
        return (rating / 5.0) * 100 * confidence;
    }

    /**
     * SCORE 3: Recency (newer products get boost)
     * Returns: 0-100
     */
    private double calculateRecencyScore(Product product) {
        LocalDateTime createdDate = product.getCreatedAt();
        if (createdDate == null) {
            return 50; // Neutral score if no date
        }
        
        long daysOld = ChronoUnit.DAYS.between(createdDate, LocalDateTime.now());
        
        // Products older than 180 days get minimum score
        if (daysOld > 180) {
            return 20;
        }
        
        // Linear decay: 100 for brand new, down to 20 at 180 days
        // Example: 30 days old = 100 - (30/180)*80 = 86.7
        return 100 - (daysOld / 180.0) * 80;
    }

    /**
     * SCORE 4: Category Affinity (user's preference for category)
     * Returns: 0-100
     */
    private double calculateCategoryAffinityScore(Product product, String userId) {
        if (userId == null) {
            return 50; // Neutral for anonymous users
        }

        // Get user's category interaction history
        Map<String, Long> categoryInteractions = 
            userBehaviorRepository.getCategoryInteractionCount(userId);
        
        if (categoryInteractions.isEmpty()) {
            return 50; // Neutral for new users
        }

        String productCategory = product.getCategory();
        long categoryCount = categoryInteractions.getOrDefault(productCategory, 0L);
        long totalInteractions = categoryInteractions.values().stream()
            .mapToLong(Long::longValue).sum();
        
        // Calculate percentage of user's interactions in this category
        // Example: User viewed 60 electronics out of 100 total = 60.0
        double affinityRatio = (double) categoryCount / totalInteractions;
        return affinityRatio * 100;
    }

    /**
     * SCORE 5: Collaborative Filtering
     * "Users who bought X also bought Y"
     * Returns: 0-100
     */
    private double calculateCollaborativeScore(Product product, String userId) {
        if (userId == null) {
            return 50; // Neutral for anonymous users
        }

        // Get user's product history
        List<String> userProducts = userBehaviorRepository.getUserProductHistory(userId);
        
        if (userProducts.isEmpty()) {
            return 50; // Neutral for new users
        }

        // Find co-occurrence (simplified for now)
        Map<String, Integer> coOccurrence = 
            userBehaviorRepository.getCoOccurrenceCount(userProducts, product.getId());
        
        if (coOccurrence.isEmpty()) {
            return 30; // Lower score if no co-occurrence
        }

        int maxCoOccurrence = coOccurrence.values().stream()
            .max(Integer::compareTo).orElse(0);
        
        // Example: Bought together 3 times = 3 * 20 = 60.0
        return Math.min(100, maxCoOccurrence * 20);
    }

    /**
     * Get similar products based on a specific product
     */
    public RecommendationResponse getSimilarProducts(String productId, int limit) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        // Find products in same category
        List<Product> similarProducts = productRepository
            .findByCategory(product.getCategory())
            .stream()
            .filter(p -> !p.getId().equals(productId))
            .limit(limit * 2)
            .collect(Collectors.toList());

        // Score based on similarity
        List<ScoredProduct> scoredProducts = similarProducts.stream()
            .map(p -> calculateSimilarityScore(product, p))
            .sorted(Comparator.comparingDouble(ScoredProduct::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return RecommendationResponse.builder()
            .userId(null)
            .recommendations(scoredProducts)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Calculate similarity between two products
     */
    private ScoredProduct calculateSimilarityScore(Product source, Product target) {
        double score = 0;

        // Same category: +30 points
        if (source.getCategory().equals(target.getCategory())) {
            score += 30;
        }

        // Similar price: +25 points
        double priceDiff = Math.abs(source.getPrice() - target.getPrice());
        double priceRatio = priceDiff / source.getPrice();
        if (priceRatio < 0.2) {
            score += 25;
        } else if (priceRatio < 0.5) {
            score += 15;
        }

        // Similar rating: +20 points
        double ratingDiff = Math.abs(source.getAverageRating() - target.getAverageRating());
        if (ratingDiff < 0.5) {
            score += 20;
        } else if (ratingDiff < 1.0) {
            score += 10;
        }

        // Popularity bonus: +25 points
        score += Math.min(25, target.getSalesCount() / 100.0);

        return ScoredProduct.builder()
            .productId(target.getId())
            .productName(target.getName())
            .category(target.getCategory())
            .price(target.getPrice())
            .imageUrl(target.getImageUrl())
            .score(score)
            .build();
    }
}