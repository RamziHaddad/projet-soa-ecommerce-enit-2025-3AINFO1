package com.example.searchservice.service;

import com.example.searchservice.dto.ProductSearchResponse;
import com.example.searchservice.model.ProductIndex;
import com.example.searchservice.repository.ProductIndexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RechercheService {

    @Autowired
    private ProductIndexRepository productIndexRepository;

    public Page<ProductSearchResponse> searchProducts(String query, String category, 
                                                 Double priceMin, Double priceMax, 
                                                 int page, int size, String sortBy, String sortDir) {
    System.out.println("Début de la recherche avec params: query=" + query + ", category=" + category + 
                       ", priceMin=" + priceMin + ", priceMax=" + priceMax + ", page=" + page + 
                       ", size=" + size + ", sortBy=" + sortBy + ", sortDir=" + sortDir);
    
    try {
        // Validation des paramètres
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20; // Limite maximale
        
        // Validation du tri
        if (!"name".equals(sortBy) && !"price".equals(sortBy) && !"category".equals(sortBy)) {
            sortBy = "name";
        }
        
        Pageable pageable;
        if ("name".equals(sortBy)) {
            // Ne pas trier par name car c'est un champ text non triable
            pageable = PageRequest.of(page, size);
        } else {
            Sort sort = Sort.by(sortBy);
            if ("desc".equalsIgnoreCase(sortDir)) {
                sort = sort.descending();
            } else {
                sort = sort.ascending();
            }
            pageable = PageRequest.of(page, size, sort);
        }
        
        Page<ProductIndex> results;
        
        // Logique de recherche selon les paramètres fournis
        if (query != null && !query.trim().isEmpty()) {
            // Recherche par nom
            if (category != null && !category.trim().isEmpty()) {
                // Nom + catégorie
                results = productIndexRepository.findByNameContainingAndCategory(query.trim(), category.trim(), pageable);
            } else if (priceMin != null || priceMax != null) {
                // Nom + prix
                Double min = priceMin != null ? priceMin : 0.0;
                Double max = priceMax != null ? priceMax : Double.MAX_VALUE;
                results = productIndexRepository.findByNameContainingAndPriceBetween(query.trim(), min, max, pageable);
            } else {
                // Juste par nom
                results = productIndexRepository.findByNameContaining(query.trim(), pageable);
            }
        } else if (category != null && !category.trim().isEmpty()) {
            // Recherche par catégorie
            results = productIndexRepository.findByCategory(category.trim(), pageable);
        } else if (priceMin != null || priceMax != null) {
            // Recherche par prix
            Double min = priceMin != null ? priceMin : 0.0;
            Double max = priceMax != null ? priceMax : Double.MAX_VALUE;
            results = productIndexRepository.findByPriceBetween(min, max, pageable);
        } else {
            // Recherche générale - tous les produits
            results = productIndexRepository.findAll(pageable);
        }
        
        System.out.println("Résultats trouvés: " + results.getTotalElements() + " éléments");
        
        // Conversion en DTO
        return results.map(ProductSearchResponse::new);
        
    } catch (Exception e) {
        System.err.println("Erreur dans searchProducts: " + e.getMessage());
        e.printStackTrace();
        throw e; // Relancer pour que le controller l'attrape
    }
}


}
