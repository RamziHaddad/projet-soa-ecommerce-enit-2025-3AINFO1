// src/main/java/com/example/searchservice/service/IndexationService.java
package com.example.searchservice.service;

import com.example.searchservice.model.Product;
import com.example.searchservice.model.ProductIndex;
import com.example.searchservice.repository.ProductIndexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class IndexationService {

    @Autowired
    private ProductIndexRepository productIndexRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${catalog.base-url:http://localhost:8081}")
    private String catalogBaseUrl;

    public void indexProducts() {
        // Étape A: Récupérer les IDs non indexés
        ResponseEntity<String[]> notIndexedResponse = restTemplate.getForEntity(catalogBaseUrl + "/products/not-indexed", String[].class);
        String[] idArray = notIndexedResponse.getBody();
        if (idArray == null || idArray.length == 0) {
            System.out.println("Aucun produit à indexer.");
            return;
        }
        List<String> ids = Arrays.asList(idArray);

        // Étape B: Marquer comme indexés (verrouiller)
        restTemplate.postForEntity(catalogBaseUrl + "/products/mark-indexed", ids, Void.class);

        for (String id : ids) {
            try {
                // Récupérer détails du produit
                ResponseEntity<Product> productResponse = restTemplate.getForEntity(catalogBaseUrl + "/products/" + id, Product.class);
                Product product = productResponse.getBody();
                if (product == null) {
                    throw new Exception("Produit non trouvé: " + id);
                }

                // Convertir en ProductIndex et indexer
                ProductIndex index = new ProductIndex(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getCategory()
                );
                productIndexRepository.save(index);

                System.out.println("Produit indexé avec succès: " + id);
                // Dans le flux décrit, pas de confirm séparé car déjà marqué

            } catch (Exception e) {
                // En cas d'erreur, unmark
                restTemplate.postForEntity(catalogBaseUrl + "/products/unmark-indexed/" + id, null, Void.class);
                System.out.println("Erreur d'indexation pour produit " + id + ": " + e.getMessage());
            }
        }
    }
}