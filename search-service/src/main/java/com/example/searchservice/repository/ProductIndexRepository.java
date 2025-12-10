// src/main/java/com/example/searchservice/repository/ProductIndexRepository.java
package com.example.searchservice.repository;

import com.example.searchservice.model.ProductIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductIndexRepository extends ElasticsearchRepository<ProductIndex, String> {
}