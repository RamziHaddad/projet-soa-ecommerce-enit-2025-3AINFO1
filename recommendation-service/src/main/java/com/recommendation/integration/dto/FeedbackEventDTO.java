package com.recommendation.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO pour les événements du service Feedback
 * Compatible avec le feedback-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackEventDTO {
    private Long feedbackId;
    private Long userId;
    private String productId;
    private Integer rating; // 1-5
    private String comment;
    private LocalDateTime createdAt;
    private String feedbackType; // RATING, REVIEW, COMPLAINT
}
