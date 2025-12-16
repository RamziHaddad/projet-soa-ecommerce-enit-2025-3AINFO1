package com.example.mockcatalogue.service;

import com.example.mockcatalogue.model.Product;
import com.example.mockcatalogue.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @PostConstruct
    public void initData() {
        // Données mock initiales (simule la BD catalogue)
        productRepository.saveAll(List.of(
                new Product("1", "Ordinateur Portable", "Puissant", 999.99, "Electronique"),
                new Product("2", "Smartphone", "Dernier modèle", 799.99, "Electronique"),
                new Product("3", "Livre Java", "Programmation", 29.99, "Livres"),
                new Product("4", "Cafétière", "Automatique", 149.99, "Maison")
        ));
        System.out.println("Données mock chargées : 4 produits.");
    }

    public List<String> getNotIndexedIds() {
        return productRepository.findNotIndexedIds().subList(0, Math.min(100, productRepository.findNotIndexedIds().size()));
    }

    public Product getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    public void markIndexed(List<String> ids) {
        productRepository.markIndexed(ids);
    }

    public void unmarkIndexed(String id) {
        productRepository.unmarkIndexed(id);
    }
}