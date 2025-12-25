package com.example.cart.controller;

import com.example.cart.service.OutboxPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/outbox")
public class OutboxController {

    private final OutboxPublisher outboxPublisher;

    public OutboxController(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @PostMapping("/flush")
    public ResponseEntity<Map<String, Object>> flush() {
        int count = outboxPublisher.publishPending();
        return ResponseEntity.ok(Map.of("published", count));
    }
}
