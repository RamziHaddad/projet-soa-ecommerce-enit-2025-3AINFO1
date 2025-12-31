package com.recommendation.feature;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbeddingEntity, String> {
}
