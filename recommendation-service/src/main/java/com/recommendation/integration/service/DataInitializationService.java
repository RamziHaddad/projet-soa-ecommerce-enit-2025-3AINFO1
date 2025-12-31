package com.recommendation.integration.service;

import com.recommendation.integration.client.CatalogServiceClient;
import com.recommendation.integration.dto.CatalogEventDTO;
import com.recommendation.ml.FeatureEngineer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * Service d'initialisation des données
 * Charge les produits du catalog-service au démarrage
 */
@Slf4j
@Service
public class DataInitializationService implements ApplicationRunner {
    
    private final CatalogServiceClient catalogServiceClient;
    private final FeatureEngineer featureEngineer;
    
    public DataInitializationService(CatalogServiceClient catalogServiceClient,
                                     FeatureEngineer featureEngineer) {
        this.catalogServiceClient = catalogServiceClient;
        this.featureEngineer = featureEngineer;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting data initialization from catalog-service...");
        
        try {
            CatalogEventDTO[] products = catalogServiceClient.getAllProducts();
            if (products != null && products.length > 0) {
                log.info("Loading {} products from catalog-service", products.length);
                
                for (CatalogEventDTO product : products) {
                    try {
                        // Extraire les features du produit
                        featureEngineer.extractProductFeatures(
                            product.getProductId(),
                            product.getName(),
                            product.getDescription(),
                            product.getPrice()
                        );
                    } catch (Exception e) {
                        log.warn("Error initializing product: {}", product.getProductId(), e);
                    }
                }
                log.info("Data initialization completed - {} products loaded", products.length);
            } else {
                log.warn("No products found in catalog-service");
            }
        } catch (Exception e) {
            log.error("Error during data initialization", e);
        }
    }
}
