package com.example.mockcatalogue.repository;

import com.example.mockcatalogue.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("SELECT p.id FROM Product p WHERE p.isIndexed = false ORDER BY p.lastUpdateTime ASC")
    List<String> findNotIndexedIds(); // Limite gérée dans service si besoin

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.isIndexed = true WHERE p.id IN ?1")
    void markIndexed(List<String> ids);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.isIndexed = false WHERE p.id = ?1")
    void unmarkIndexed(String id);
}