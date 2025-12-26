package com.enit.catalog.repository;

import com.enit.catalog.entity.OutBoxEvent;
import com.enit.catalog.entity.enums.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutBoxEventRepository extends JpaRepository<OutBoxEvent, Long> {

    List<OutBoxEvent> findByStatusOrderByCreatedAtAsc(Status status);

    @Query("SELECT o FROM OutBoxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutBoxEvent> findByStatusWithLimit(@Param("status") Status status, Pageable pageable);

    @Query("SELECT o FROM OutBoxEvent o WHERE o.status = 'FAILED' AND o.retryCount < o.maxRetries ORDER BY o.createdAt ASC")
    List<OutBoxEvent> findRetryableEvents(Pageable pageable);

    @Modifying
    @Query("UPDATE OutBoxEvent o SET o.status = :status, o.processedAt = :processedAt WHERE o.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Status status, @Param("processedAt") LocalDateTime processedAt);

    long countByStatus(Status status);

    @Query("SELECT o FROM OutBoxEvent o WHERE o.status = 'PROCESSING' AND o.processedAt < :threshold")
    List<OutBoxEvent> findStuckProcessingEvents(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE OutBoxEvent o SET o.status = 'PENDING', o.retryCount = o.retryCount + 1 WHERE o.status = 'PROCESSING' AND o.processedAt < :threshold")
    int resetStuckEvents(@Param("threshold") LocalDateTime threshold);
}
