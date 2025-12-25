package com.example.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cart.entity.Cart;


public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerId(Long customerId);
}
