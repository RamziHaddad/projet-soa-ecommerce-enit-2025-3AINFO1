package com.onlineshop.order.repository;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {
    
    Optional<SagaState> findByOrder(Order order);
    
    List<SagaState> findByStatus(SagaStatus status);
}
