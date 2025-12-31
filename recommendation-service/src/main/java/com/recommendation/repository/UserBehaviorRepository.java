package com.recommendation.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for user behavior analytics
 * Provides aggregated statistics about user behavior
 */
@Repository
public interface UserBehaviorRepository {
    
    long countEventsByUserId(String userId);
    
    double getAvgSessionDuration(String userId);
    
    long countPurchasesByUserId(String userId);
    
    double getAvgOrderValue(String userId);
    
    double getAccountAgeDays(String userId);
    
    boolean hasUserViewedProduct(String userId, String productId);
    
    boolean hasUserViewedCategory(String userId, String category);
    
    double getUserProductViewCount(String userId, String productId);
    
    double getUserBudget(String userId);
    
    List<String> getUserTopCategories(String userId, int limit);
}
