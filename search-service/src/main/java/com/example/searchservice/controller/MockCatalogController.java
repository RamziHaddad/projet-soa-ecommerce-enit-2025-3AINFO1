// src/main/java/com/example/searchservice/controller/MockCatalogController.java
package com.example.searchservice.controller;

import com.example.searchservice.model.Product;
import com.example.searchservice.service.MockCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class MockCatalogController {

    @Autowired
    private MockCatalogService mockCatalogService;

    @GetMapping("/not-indexed")
    public ResponseEntity<List<String>> getNotIndexed() {
        return ResponseEntity.ok(mockCatalogService.getNotIndexedIds());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        Product product = mockCatalogService.getProductById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/mark-indexed")
    public ResponseEntity<Void> markIndexed(@RequestBody List<String> ids) {
        mockCatalogService.markIndexed(ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unmark-indexed/{id}")
    public ResponseEntity<Void> unmarkIndexed(@PathVariable String id) {
        mockCatalogService.unmarkIndexed(id);
        return ResponseEntity.ok().build();
    }
}