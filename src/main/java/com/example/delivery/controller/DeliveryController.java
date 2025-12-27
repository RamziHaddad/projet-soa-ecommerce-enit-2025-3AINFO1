package com.example.delivery.controller;

import com.example.delivery.dto.DeliveryRequest;
import com.example.delivery.dto.DeliveryResponse;
import com.example.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeliveryResponse> createDelivery(@Valid @RequestBody DeliveryRequest request) {
        DeliveryResponse response = service.createDelivery(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResponse> getByOrderId(@PathVariable Long orderId) {
        DeliveryResponse response = service.getByOrderId(orderId);
        if (response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryResponse> updateStatus(@PathVariable Long id, @RequestBody String status) {
        DeliveryResponse response = service.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancelDelivery(@PathVariable Long id) {
		boolean canceled = service.cancelDelivery(id);
		if (canceled) return ResponseEntity.ok().build();
		return ResponseEntity.notFound().build();
	}


	@GetMapping("/hello")
    public String hello() {
        return "Hello, Delivery API is running!";
    }
}
