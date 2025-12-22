package com.onlineshop.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {

    Optional<SagaState> findByOrder(Order order);

    // Find sagas that are ready to retry (not yet marked)
    @Query("""
            SELECT s FROM SagaState s
            WHERE s.status = :status
              AND s.nextRetryTime <= :now
              AND s.retryable = true
            """)
    List<SagaState> findReadyForRetry(@Param("status") SagaStatus status,
            @Param("now") LocalDateTime now);

    // Mark specific saga as RETRYING (to avoid duplicates)
    @Modifying
    @Transactional
    @Query("UPDATE SagaState s SET s.status = :newStatus WHERE s.id = :id AND s.status = :expectedStatus")
    int markAsRetrying(@Param("id") Long id,
            @Param("newStatus") SagaStatus newStatus,
            @Param("expectedStatus") SagaStatus expectedStatus);

    // For stuck sagas: find failed sagas older than cutoff with retryable = true
    @Query("""
            SELECT s FROM SagaState s
            WHERE s.status = :status
              AND s.updatedAt <= :cutoff
              AND s.retryable = true
            """)
    List<SagaState> findStuckSagas(@Param("status") SagaStatus status,
            @Param("cutoff") LocalDateTime cutoff);

    List<SagaState> findByStatus(SagaStatus status);
}