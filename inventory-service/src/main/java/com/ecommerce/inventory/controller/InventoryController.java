package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.dto.ReserveInventoryResponse;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.Reservation;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Reservation should be confirmed by the orchestrator
     */
    @PostMapping("/reservations")
    public ResponseEntity<ReserveInventoryResponse> reserveInventory(
            @RequestBody ReserveInventoryRequest request) {
        ReserveInventoryResponse response = inventoryService.reserveInventory(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reservation concellation with a compensation action
     */
    @PostMapping("/reservations/{orderId}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable String orderId) {
        inventoryService.cancelReservation(orderId);
        return ResponseEntity.ok().build();
    }

    /**
     * Reservation should be confirme in a fianl step
     */
    @PostMapping("/reservations/{orderId}/confirm")
    public ResponseEntity<Void> confirmReservation(@PathVariable String orderId) {
        inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok().build();
    }

    /**
     * Track inventory counters for a given product
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(@PathVariable String productId) {
        return inventoryRepository.findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get reservation status by orderId
     */
    @GetMapping("/reservations/{orderId}")
    public ResponseEntity<Reservation> getReservation(@PathVariable String orderId) {
        return reservationRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * List all inventory items (simple admin view)
     */
    @GetMapping
    public ResponseEntity<Iterable<Inventory>> listAll() {
        return ResponseEntity.ok(inventoryRepository.findAll());
    }
}
