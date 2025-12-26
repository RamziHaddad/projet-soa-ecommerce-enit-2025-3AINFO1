package com.enit.catalog.mapper;

import com.enit.catalog.dto.request.RequestSearch;
import com.enit.catalog.entity.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxPayloadMapper {

    private final ObjectMapper objectMapper;

    public String toCreateUpdatePayload(Product product) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", product.getId());
        payload.put("name", product.getName());
        payload.put("description", product.getDescription());
        payload.put("price", product.getPrice());
        payload.put("imageUrl", product.getImageUrl());
        payload.put("createdAt", product.getCreatedAt() != null ? product.getCreatedAt().toString() : null);
        payload.put("updatedAt", product.getUpdatedAt() != null ? product.getUpdatedAt().toString() : null);

        return toJson(payload);
    }
    public String toDeletePayload(Long productId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("deletedAt", LocalDateTime.now().toString());

        return toJson(payload);
    }
    
    public RequestSearch toRequestSearch(String jsonPayload, boolean includeDetails) {
        try {
            Map<String, Object> map = objectMapper.readValue(jsonPayload, Map.class);
            
            RequestSearch request = new RequestSearch();
            
            if (map.containsKey("productId")) {
                request.setProductId(((Number) map.get("productId")).longValue());
            }
            
            if (includeDetails) {
                request.setName((String) map.get("name"));
                request.setDescription((String) map.get("description"));
                if (map.get("price") != null) {
                    request.setPrice(((Number) map.get("price")).doubleValue());
                }
                request.setImageUrl((String) map.get("imageUrl"));
            }
            
            return request;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de parsing du payload JSON", e);
        }
    }

    /**
     * Convertit une Map en JSON
     */
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de sérialisation JSON", e);
        }
    }
}
