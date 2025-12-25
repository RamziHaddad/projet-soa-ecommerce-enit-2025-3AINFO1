package com.ecommerce.recommendation.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class UserBehaviorRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Map<String, Long> getCategoryInteractionCount(String userId) {
        try {
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)),
                Aggregation.group("category").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "count")
            );

            AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "user_behaviors", Map.class);

            return results.getMappedResults().stream()
                .collect(Collectors.toMap(
                    m -> (String) m.get("_id"),
                    m -> ((Number) m.get("count")).longValue()
                ));
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public List<String> getUserProductHistory(String userId) {
        try {
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)),
                Aggregation.group("productId"),
                Aggregation.limit(50)
            );

            AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "user_behaviors", Map.class);

            return results.getMappedResults().stream()
                .map(m -> (String) m.get("_id"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    public Map<String, Integer> getCoOccurrenceCount(List<String> userProducts, String targetProductId) {
        return new HashMap<>();
    }
}