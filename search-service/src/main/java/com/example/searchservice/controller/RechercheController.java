package com.example.searchservice.controller;

import com.example.searchservice.dto.ProductSearchResponse;
import com.example.searchservice.service.RechercheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class RechercheController {

    @Autowired
    private RechercheService rechercheService;

    @GetMapping
    public ResponseEntity<Page<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double priceMin,
            @RequestParam(required = false) Double priceMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            // Validation des paramètres de tri
            if (!"name".equals(sortBy) && !"price".equals(sortBy) && !"category".equals(sortBy)) {
                sortBy = "name";
            }

            if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
                sortDir = "asc";
            }

            Page<ProductSearchResponse> results = rechercheService.searchProducts(
                query, category, priceMin, priceMax, page, size, sortBy, sortDir);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche: " + e.getMessage());
            e.printStackTrace();
            // Retourner une réponse plus détaillée si possible
            return ResponseEntity.internalServerError().body(null);
        }

    }
}
