package com.example.searchservice.repository;

import com.example.searchservice.model.ProductIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductIndexRepository extends ElasticsearchRepository<ProductIndex, String> {
    
    // Recherche par nom (contient)
    Page<ProductIndex> findByNameContaining(String name, Pageable pageable);
    
    // Recherche par catégorie exacte
    Page<ProductIndex> findByCategory(String category, Pageable pageable);
    
    // Recherche par plage de prix
    Page<ProductIndex> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);
    
    // Recherche combinée : nom + catégorie
    Page<ProductIndex> findByNameContainingAndCategory(String name, String category, Pageable pageable);
    
    // Recherche combinée : nom + plage de prix
    Page<ProductIndex> findByNameContainingAndPriceBetween(String name, Double minPrice, Double maxPrice, Pageable pageable);
}
