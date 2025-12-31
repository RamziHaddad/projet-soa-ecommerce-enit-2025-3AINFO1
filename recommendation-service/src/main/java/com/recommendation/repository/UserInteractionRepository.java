package com.recommendation.repository;

import com.recommendation.entity.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
    
    @Query("SELECT u FROM UserInteraction u ORDER BY u.userId, u.timestamp ASC")
    List<UserInteraction> findAllOrderByUserAndTimestamp();
    
    @Query("SELECT u FROM UserInteraction u WHERE u.userId = :userId ORDER BY u.timestamp ASC")
    List<UserInteraction> findByUserIdOrderByTimestamp(String userId);
}
