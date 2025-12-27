package com.enit.catalog.controller;

import com.enit.catalog.entity.enums.Status;
import com.enit.catalog.repository.OutBoxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/outbox")
@RequiredArgsConstructor
public class OutboxMonitoringController {

    private final OutBoxEventRepository outBoxEventRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOutboxStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("pending", outBoxEventRepository.countByStatus(Status.PENDING));
        stats.put("processing", outBoxEventRepository.countByStatus(Status.PROCESSING));
        stats.put("succeeded", outBoxEventRepository.countByStatus(Status.SUCCEEDED));
        stats.put("failed", outBoxEventRepository.countByStatus(Status.FAILED));
        stats.put("deadLetter", outBoxEventRepository.countByStatus(Status.DEAD_LETTER));
        stats.put("total", outBoxEventRepository.count());
        
        return ResponseEntity.ok(stats);
    }
    @GetMapping("/failed")
    public ResponseEntity<?> getFailedEvents() {
        return ResponseEntity.ok(
            outBoxEventRepository.findByStatusOrderByCreatedAtAsc(Status.FAILED)
        );
    }
    @GetMapping("/dead-letter")
    public ResponseEntity<?> getDeadLetterEvents() {
        return ResponseEntity.ok(
            outBoxEventRepository.findByStatusOrderByCreatedAtAsc(Status.DEAD_LETTER)
        );
    }
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingEvents() {
        return ResponseEntity.ok(
            outBoxEventRepository.findByStatusOrderByCreatedAtAsc(Status.PENDING)
        );
    }
}
