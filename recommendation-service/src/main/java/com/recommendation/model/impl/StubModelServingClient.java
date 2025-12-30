package com.recommendation.model.impl;

import com.recommendation.domain.Candidate;
import com.recommendation.domain.Recommendation;
import com.recommendation.ml.MLRankingEngine;
import com.recommendation.model.ModelServingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.Map;

/**
 * Model Serving Client
 * Délègue au MLRankingEngine pour le scoring avancé
 */
@Slf4j
public class StubModelServingClient implements ModelServingClient {
    
    @Autowired(required = false)
    @Lazy
    private MLRankingEngine mlRankingEngine;
    
    @Override
    public List<Recommendation> score(String userId, float[] userEmbedding, List<Candidate> candidates, Map<String, Object> context, int k) {
        
        // Si MLRankingEngine disponible, l'utiliser
        if (mlRankingEngine != null) {
            log.debug("Using ML Ranking Engine for scoring");
            return mlRankingEngine.rankCandidates(userId, userEmbedding, candidates, context, k);
        }
        
        // Fallback: Simple rule-based scoring
        log.debug("Using fallback rule-based scoring");
        return simpleRuleBasedRanking(userId, userEmbedding, candidates, context, k);
    }
    
    /**
     * Fallback: Simple rule-based ranking (ancien comportement)
     */
    private List<Recommendation> simpleRuleBasedRanking(
            String userId, 
            float[] userEmbedding, 
            List<Candidate> candidates, 
            Map<String, Object> context, 
            int k) {
        
        boolean isExperiment = context != null && "true".equals(context.get("ab_experiment"));
        
        double seasonalBoost = 0.0;
        Object season = context != null ? context.get("season") : null;
        if (season != null) {
            String s = season.toString().toLowerCase();
            if (s.contains("ramadan")) seasonalBoost = 0.2;
            else if (s.contains("winter")) seasonalBoost = 0.1;
        }
        
        final double boost = seasonalBoost;
        final double experimentBoost = isExperiment ? 0.15 : 0.0;
        
        return candidates.stream()
                .map(c -> {
                    double score = c.similarity() + boost + experimentBoost;
                    return new Recommendation(c.productId(), Math.max(0.0, score));
                })
                .sorted((r1, r2) -> Double.compare(r2.score(), r1.score()))
                .limit(k)
                .toList();
    }
}
