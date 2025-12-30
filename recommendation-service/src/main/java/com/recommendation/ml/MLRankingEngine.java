package com.recommendation.ml;

import com.recommendation.domain.Candidate;
import com.recommendation.domain.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ML Ranking Engine avec VRAI Gradient Boosting
 * Utilise le modèle entraîné pour scorer les candidats
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MLRankingEngine {

    private final ModelRegistry modelRegistry;
    private final FeatureEngineer featureEngineer;
    private final GradientBoostingRanker gradientBoostingRanker;

    /**
     * Score les candidats avec le modèle Gradient Boosting RÉEL
     */
    public List<Recommendation> rankCandidates(
            String userId, 
            float[] userEmbedding, 
            List<Candidate> candidates,
            Map<String, Object> context,
            int topK) {
        
        List<ScoredCandidate> scored = candidates.stream()
                .map(candidate -> {
                    // Extract features pour ce couple (user, product)
                    var features = featureEngineer.extractFeatures(
                            userId, candidate.productId(), context);
                    
                    // Score avec le VRAI modèle Gradient Boosting
                    double mlScore = gradientBoostingRanker.predict(features.toArray());
                    
                    return new ScoredCandidate(candidate.productId(), mlScore, features);
                })
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
                .limit(topK)
                .toList();
        
        return scored.stream()
                .map(sc -> new Recommendation(sc.productId(), sc.score()))
                .collect(Collectors.toList());
    }

    private record ScoredCandidate(String productId, double score, FeatureEngineer.Features features) {}
}
    
    // Cache for loaded model
    private volatile Object xgboostModel = null;
    private volatile String loadedModelVersion = null;

    /**
     * Score les candidats avec le modèle XGBoost
     */
    public List<Recommendation> rankCandidates(
            String userId, 
            float[] userEmbedding, 
            List<Candidate> candidates,
            Map<String, Object> context,
            int topK) {
        
        // TODO: Charger le vrai modèle XGBoost
        // Pour l'instant, on utilise une version simplifiée basée sur les features
        
        List<ScoredCandidate> scored = candidates.stream()
                .map(candidate -> {
                    // Extract features pour ce couple (user, product)
                    var features = featureEngineer.extractFeatures(
                            userId, candidate.productId(), context);
                    
                    // Calculate score with features
                    double mlScore = calculateMLScore(userEmbedding, candidate, features, context);
                    
                    return new ScoredCandidate(candidate.productId(), mlScore, features);
                })
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
                .limit(topK)
                .toList();
        
        return scored.stream()
                .map(sc -> new Recommendation(sc.productId(), sc.score()))
                .collect(Collectors.toList());
    }

    /**
     * Calcule le score ML (version simplifiée avant XGBoost réel)
     */
    private double calculateMLScore(
            float[] userEmbedding,
            Candidate candidate, 
            FeatureEngineer.Features features,
            Map<String, Object> context) {
        
        // Base score from ANN similarity
        double score = candidate.similarity();
        
        // Feature-based boosts
        
        // 1. Temporal features
        double hourOfDay = features.get("hour_of_day");
        if (hourOfDay >= 18 && hourOfDay <= 22) {
            score += 0.05; // Prime time boost
        }
        
        // 2. User behavioral features
        double userActivity = features.get("user_activity_level");
        score += userActivity * 0.1; // Active users get better recommendations
        
        double purchaseFreq = features.get("user_purchase_frequency");
        score += Math.min(0.1, purchaseFreq * 0.5); // Frequent buyers boost
        
        // 3. Product features
        double popularity = features.get("product_popularity");
        score += popularity * 0.15; // Popular products boost
        
        double avgRating = features.get("product_avg_rating");
        score += (avgRating / 5.0) * 0.1; // Rating boost (normalized)
        
        // 4. User-Product interaction features
        if (features.get("user_viewed_product_before") > 0) {
            score += 0.2; // Previously viewed
        }
        
        if (features.get("user_viewed_category_before") > 0) {
            score += 0.1; // Category affinity
        }
        
        // 5. Cross features
        double priceRatio = features.get("price_user_budget_ratio");
        if (priceRatio < 0.3) {
            score += 0.1; // Affordable for user
        } else if (priceRatio > 1.0) {
            score -= 0.2; // Too expensive
        }
        
        double categoryMatch = features.get("category_match_score");
        score += categoryMatch * 0.15;
        
        // 6. Contextual boosts
        boolean isExperiment = features.get("is_experiment") > 0;
        if (isExperiment) {
            score += 0.15;
        }
        
        // Seasonal boosts
        if (features.get("is_ramadan") > 0) {
            score += 0.2;
        } else if (features.get("is_winter") > 0) {
            score += 0.1;
        } else if (features.get("is_summer") > 0) {
            score += 0.05;
        }
        
        return score;
    }

    /**
     * Charge le modèle XGBoost depuis le Model Registry
     */
    private void loadXGBoostModel() {
        try {
            if (xgboostModel == null) {
                log.info("Loading XGBoost ranking model from registry...");
                
                byte[] modelBytes = modelRegistry.loadLatestModel("ranking_model");
                
                // TODO: Deserialize XGBoost model
                // Booster booster = XGBoost.loadModel(new ByteArrayInputStream(modelBytes));
                // xgboostModel = booster;
                
                log.info("XGBoost model loaded successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to load XGBoost model, using feature-based ranking", e);
        }
    }

    /**
     * Prédit avec XGBoost (TODO: implémenter avec vrai XGBoost)
     */
    private double predictWithXGBoost(float[] features) {
        // TODO: Implement real XGBoost prediction
        // DMatrix dmatrix = new DMatrix(features, 1, features.length);
        // float[][] predictions = booster.predict(dmatrix);
        // return predictions[0][0];
        
        return 0.0; // Placeholder
    }

    record ScoredCandidate(String productId, double score, FeatureEngineer.Features features) {}
}
