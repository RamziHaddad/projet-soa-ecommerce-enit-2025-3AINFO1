package com.recommendation.repository.impl;

import com.recommendation.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Implémentation stub du ProductRepository
 * TODO: Remplacer par de vraies requêtes SQL/JPA
 */
@Slf4j
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final Random random = new Random();
    private final Map<String, String> productCategories = new HashMap<>();

    public ProductRepositoryImpl() {
        // Seed some product categories
        productCategories.put("prod_1", "electronics");
        productCategories.put("prod_2", "clothing");
        productCategories.put("prod_3", "food");
        productCategories.put("prod_4", "books");
        productCategories.put("prod_5", "home");
    }

    @Override
    public long getViewCount(String productId) {
        return 100 + random.nextInt(10000);
    }

    @Override
    public double getAvgRating(String productId) {
        return 3.0 + random.nextDouble() * 2.0; // 3.0 - 5.0
    }

    @Override
    public double getPrice(String productId) {
        return 10.0 + random.nextDouble() * 500.0;
    }

    @Override
    public double getStockLevel(String productId) {
        return random.nextDouble(); // 0.0 - 1.0 (normalized)
    }

    @Override
    public double getProductAgeDays(String productId) {
        return random.nextDouble() * 365.0;
    }

    @Override
    public String getCategory(String productId) {
        return productCategories.getOrDefault(productId, "general");
    }
}
