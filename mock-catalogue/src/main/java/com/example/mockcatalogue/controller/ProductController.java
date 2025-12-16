package com.example.mockcatalogue.controller;

import com.example.mockcatalogue.model.Product;
import com.example.mockcatalogue.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/not-indexed")
    public ResponseEntity<List<String>> getNotIndexed() {
        return ResponseEntity.ok(productService.getNotIndexedIds());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        Product product = productService.getProductById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/mark-indexed")
    public ResponseEntity<Void> markIndexed(@RequestBody List<String> ids) {
        productService.markIndexed(ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unmark-indexed/{id}")
    public ResponseEntity<Void> unmarkIndexed(@PathVariable String id) {
        productService.unmarkIndexed(id);
        return ResponseEntity.ok().build();
    }
}