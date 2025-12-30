package com.recommendation.integration.client;

import com.recommendation.integration.dto.CatalogEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Client REST pour le Catalog Service
 * Récupère les produits en temps quasi-réel
 */
@Slf4j
@Service
public class CatalogServiceClient {
    
    @Value("${integration.catalog-service.url:http://localhost:8081}")
    private String catalogServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public CatalogServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Récupère les détails d'un produit
     */
    public CatalogEventDTO getProduct(String productId) {
        try {
            String url = catalogServiceUrl + "/api/products/" + productId;
            CatalogEventDTO product = restTemplate.getForObject(url, CatalogEventDTO.class);
            log.debug("Retrieved product from catalog: {}", productId);
            return product;
        } catch (Exception e) {
            log.error("Error retrieving product from catalog: {}", productId, e);
            return null;
        }
    }
    
    /**
     * Récupère tous les produits (pour le seed initial)
     */
    public CatalogEventDTO[] getAllProducts() {
        try {
            String url = catalogServiceUrl + "/api/products";
            CatalogEventDTO[] products = restTemplate.getForObject(url, CatalogEventDTO[].class);
            log.info("Retrieved {} products from catalog", products != null ? products.length : 0);
            return products;
        } catch (Exception e) {
            log.error("Error retrieving products from catalog", e);
            return new CatalogEventDTO[0];
        }
    }
}
