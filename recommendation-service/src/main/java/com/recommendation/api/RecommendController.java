package com.recommendation.api;

import com.recommendation.api.dto.RecommendRequest;
import com.recommendation.api.dto.RecommendResponse;
import com.recommendation.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RecommendController {
    private final RecommendationService recommendationService;

    public RecommendController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommend")
    public ResponseEntity<RecommendResponse> recommend(@RequestBody RecommendRequest request) {
        return ResponseEntity.ok(recommendationService.recommend(request));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
