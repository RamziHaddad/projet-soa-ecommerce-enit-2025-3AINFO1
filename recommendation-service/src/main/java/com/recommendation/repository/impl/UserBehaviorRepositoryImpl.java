package com.recommendation.repository.impl;

import com.recommendation.repository.UserBehaviorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Implémentation stub du UserBehaviorRepository
 * TODO: Remplacer par de vraies requêtes SQL/JPA
 */
@Slf4j
@Component
public class UserBehaviorRepositoryImpl implements UserBehaviorRepository {

    private final Random random = new Random();

    @Override
    public long countEventsByUserId(String userId) {
        // Stub: retourne une valeur aléatoire
        return 10 + random.nextInt(100);
    }

    @Override
    public double getAvgSessionDuration(String userId) {
        // Stub: durée moyenne en secondes
        return 180.0 + random.nextDouble() * 600.0;
    }

    @Override
    public long countPurchasesByUserId(String userId) {
        return random.nextInt(20);
    }

    @Override
    public double getAvgOrderValue(String userId) {
        return 50.0 + random.nextDouble() * 200.0;
    }

    @Override
    public double getAccountAgeDays(String userId) {
        return 30.0 + random.nextDouble() * 365.0;
    }

    @Override
    public boolean hasUserViewedProduct(String userId, String productId) {
        return random.nextBoolean();
    }

    @Override
    public boolean hasUserViewedCategory(String userId, String category) {
        return random.nextBoolean();
    }

    @Override
    public double getUserProductViewCount(String userId, String productId) {
        return random.nextInt(10);
    }

    @Override
    public double getUserBudget(String userId) {
        return 100.0 + random.nextDouble() * 500.0;
    }

    @Override
    public List<String> getUserTopCategories(String userId, int limit) {
        List<String> categories = Arrays.asList(
                "electronics", "clothing", "food", "books", "home", "sports"
        );
        return categories.subList(0, Math.min(limit, categories.size()));
    }
}
