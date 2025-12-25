package com.ecommerce.recommendation.repository;

import com.ecommerce.recommendation.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    @Query("{ 'active': true }")
    List<Product> findAllActive();
    
    @Query("{ 'category': ?0, 'active': true }")
    List<Product> findByCategory(String category);
}