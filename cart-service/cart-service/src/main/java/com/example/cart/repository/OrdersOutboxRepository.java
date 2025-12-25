package com.example.cart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cart.entity.OrdersOutbox;

public interface OrdersOutboxRepository extends JpaRepository<OrdersOutbox, Long> {
    List<OrdersOutbox> findByPublishedFalseOrderByCreatedAtAsc();
}

