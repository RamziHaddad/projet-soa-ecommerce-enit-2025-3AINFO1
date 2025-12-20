package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.dto.ReserveInventoryResponse;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.Reservation;
import com.ecommerce.inventory.model.ReservationStatus;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Reserve inventory - IDEMPOTENT operation
     * Called by Saga Orchestrator
     */
    @Transactional
    public ReserveInventoryResponse reserveInventory(ReserveInventoryRequest request) {
        log.info("Reserving inventory for orderId: {}", request.getOrderId());

        // Idempotency check
        Optional<Reservation> existingReservation = reservationRepository.findByOrderId(request.getOrderId());

        if (existingReservation.isPresent()) {
            log.info("Reservation already exists for orderId: {}", request.getOrderId());
            return buildSuccessResponse(existingReservation.get());
        }

        String productId = request.getItems().get(0).getProductId();
        Integer quantity = request.getItems().get(0).getQuantity();

        // Pessimistic locking for concurrency control
        Inventory inventory = inventoryRepository
                .findByProductIdWithLock(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Check availability
        if (inventory.getAvailableQuantity() < quantity) {
            log.warn("Insufficient stock for product: {}, requested: {}, available: {}",
                    productId, quantity, inventory.getAvailableQuantity());
            throw new InsufficientStockException("Insufficient stock for product: " + productId);
        }

        // Update inventory counters
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        // Create reservation
        Reservation reservation = Reservation.builder()
                .reservationId(UUID.randomUUID())
                .orderId(request.getOrderId())
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .build();

        reservationRepository.save(reservation);

        log.info("Inventory reserved successfully for orderId: {}", request.getOrderId());
        return buildSuccessResponse(reservation);
    }

    /**
     * Cancel reservation - IDEMPOTENT compensation operation
     * Called by Saga Orchestrator on failure
     */
    @Transactional
    public void cancelReservation(String orderId) {
        log.info("Cancelling reservation for orderId: {}", orderId);

        Optional<Reservation> reservationOpt = reservationRepository.findByOrderId(orderId);

        if (reservationOpt.isEmpty()) {
            log.info("No reservation found for orderId: {}, treating as idempotent success", orderId);
            return;
        }

        Reservation reservation = reservationOpt.get();

        // Idempotency: if already cancelled or confirmed, skip
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.info("Reservation already cancelled for orderId: {}", orderId);
            return;
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            log.warn("Cannot cancel confirmed reservation for orderId: {}", orderId);
            return;
        }

        // Restore inventory
        Inventory inventory = inventoryRepository
                .findByProductIdWithLock(reservation.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + reservation.getProductId()));

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);

        // Update reservation status
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("Reservation cancelled successfully for orderId: {}", orderId);
    }

    /**
     * Confirm reservation - Final step, cannot be undone
     * Called by Saga Orchestrator on success
     */
    @Transactional
    public void confirmReservation(String orderId) {
        log.info("Confirming reservation for orderId: {}", orderId);

        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Reservation not found for orderId: " + orderId));

        // Idempotency check
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            log.info("Reservation already confirmed for orderId: {}", orderId);
            return;
        }

        Inventory inventory = inventoryRepository
                .findByProductIdWithLock(reservation.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + reservation.getProductId()));

        // Move from reserved to sold (decrease reserved_quantity)
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        log.info("Reservation confirmed successfully for orderId: {}", orderId);
    }

    private ReserveInventoryResponse buildSuccessResponse(Reservation reservation) {
        return ReserveInventoryResponse.builder()
                .success(true)
                .reservationId(reservation.getReservationId())
                .orderId(reservation.getOrderId())
                .message("Inventory reserved successfully")
                .build();
    }
}
