package com.recommendation.feature;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEmbeddingRepository extends JpaRepository<UserEmbeddingEntity, String> {
}
