package com.recommendation.integration.client;

import com.recommendation.integration.dto.UserProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client REST pour le User Service
 * Récupère le profil utilisateur (budget, location, preferences)
 */
@Slf4j
@Service
public class UserServiceClient {
    
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    
    public UserServiceClient(RestTemplate restTemplate,
                            @Value("${integration.user-service.url:http://user-service:8083}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }
    
    /**
     * Récupère le profil complet d'un utilisateur
     */
    public UserProfileDTO getUserProfile(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId + "/profile";
            UserProfileDTO profile = restTemplate.getForObject(url, UserProfileDTO.class);
            log.debug("Retrieved profile for user {}: budget={}, location={}", 
                userId, profile.getBudget(), profile.getLocation());
            return profile;
        } catch (Exception e) {
            log.warn("Failed to retrieve user profile for {}: {}", userId, e.getMessage());
            return getFallbackProfile(userId);
        }
    }
    
    /**
     * Récupère l'historique d'achats d'un utilisateur
     */
    public List<String> getUserPurchaseHistory(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId + "/purchases";
            @SuppressWarnings("unchecked")
            List<String> purchases = restTemplate.getForObject(url, List.class);
            log.debug("Retrieved {} purchases for user {}", purchases.size(), userId);
            return purchases;
        } catch (Exception e) {
            log.warn("Failed to retrieve purchase history for {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Récupère les préférences utilisateur
     */
    public List<String> getUserPreferences(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId + "/preferences";
            @SuppressWarnings("unchecked")
            List<String> preferences = restTemplate.getForObject(url, List.class);
            return preferences;
        } catch (Exception e) {
            log.warn("Failed to retrieve preferences for {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Fallback profile si le service est indisponible
     */
    private UserProfileDTO getFallbackProfile(String userId) {
        UserProfileDTO fallback = new UserProfileDTO();
        fallback.setUserId(userId);
        fallback.setBudget(100.0); // Budget par défaut
        fallback.setLocation("tunis"); // Location par défaut
        fallback.setActivityLevel("medium");
        fallback.setPurchaseFrequency(0.5);
        log.warn("Using fallback profile for user {}", userId);
        return fallback;
    }
}
