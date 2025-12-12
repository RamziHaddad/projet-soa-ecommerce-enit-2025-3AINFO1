// src/main/java/com/example/searchservice/service/MockCatalogService.java
package com.example.searchservice.service;

import com.example.searchservice.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MockCatalogService {

    private List<Product> products = new ArrayList<>();

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Product[] loadedProducts = mapper.readValue(
            getClass().getClassLoader().getResourceAsStream("mock-products.json"),
            Product[].class
        );
            products.addAll(List.of(loadedProducts));
            System.out.println("Données mock du catalogue chargées: " + products.size() + " produits.");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de mock-products.json: " + e.getMessage());
        }
    }

    public List<String> getNotIndexedIds() {
        return products.stream()
                .filter(p -> !p.isIndexed())
                .sorted(Comparator.comparing(Product::getLastUpdateTime))
                .limit(100)
                .map(Product::getId)
                .collect(Collectors.toList());
    }

    public Product getProductById(String id) {
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void markIndexed(List<String> ids) {
        products.forEach(p -> {
            if (ids.contains(p.getId())) {
                p.setIndexed(true);
            }
        });
    }

    public void unmarkIndexed(String id) {
        products.forEach(p -> {
            if (p.getId().equals(id)) {
                p.setIndexed(false);
            }
        });
    }
}