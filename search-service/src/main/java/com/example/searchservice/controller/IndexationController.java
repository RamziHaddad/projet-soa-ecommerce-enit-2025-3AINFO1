// src/main/java/com/example/searchservice/controller/IndexationController.java
package com.example.searchservice.controller;

import com.example.searchservice.service.IndexationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/index")
public class IndexationController {

    @Autowired
    private IndexationService indexationService;

    @PostMapping("/all")
    public ResponseEntity<String> indexAllProducts() {
        try {
            indexationService.indexProducts();
            return ResponseEntity.ok("Indexation des produits terminée avec succès.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'indexation: " + e.getMessage());
        }
    }
}
