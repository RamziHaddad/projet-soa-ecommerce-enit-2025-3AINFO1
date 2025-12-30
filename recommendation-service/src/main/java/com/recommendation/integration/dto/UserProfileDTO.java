package com.recommendation.integration.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO pour le profil utilisateur
 */
@Data
public class UserProfileDTO {
    private String userId;
    private Double budget;
    private String location; // tunis, sfax, sousse, etc.
    private String activityLevel; // low, medium, high
    private Double purchaseFrequency; // 0-10 (achats par mois)
    private List<String> preferredCategories;
    private List<String> traditions; // ramadan, winter, summer, etc.
}
