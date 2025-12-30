package com.recommendation.repository;

import org.springframework.stereotype.Repository;

/**
 * Repository for product catalog data
 */
@Repository
public interface ProductRepository {
    
    long getViewCount(String productId);
    
    double getAvgRating(String productId);
    
    double getPrice(String productId);
    
    double getStockLevel(String productId);
    
    double getProductAgeDays(String productId);
    
    String getCategory(String productId);
}
