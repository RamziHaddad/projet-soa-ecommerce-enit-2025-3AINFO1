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

        // Idempotency: if any reservations already exist for this order, return them
        var existing = reservationRepository.findAllByOrderId(request.getOrderId());
        if (!existing.isEmpty()) {
            log.info("Reservations already exist for orderId: {} ({} items)", request.getOrderId(), existing.size());
            return buildSuccessResponse(existing);
        }

        // Validate request
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No items provided for reservation");
        }

        // Phase 1: Lock and validate availability for all items
        for (ReserveInventoryRequest.ItemRequest item : request.getItems()) {
            Inventory inv = inventoryRepository
                    .findByProductIdWithLock(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
            if (inv.getAvailableQuantity() < item.getQuantity()) {
                log.warn("Insufficient stock for product: {}, requested: {}, available: {}",
                        item.getProductId(), item.getQuantity(), inv.getAvailableQuantity());
                throw new InsufficientStockException("Insufficient stock for product: " + item.getProductId());
            }
        }

        // Phase 2: Apply all updates and create reservations
        var savedReservations = new java.util.ArrayList<Reservation>(request.getItems().size());
        for (ReserveInventoryRequest.ItemRequest item : request.getItems()) {
            Inventory inv = inventoryRepository
                    .findByProductIdWithLock(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
            inv.setAvailableQuantity(inv.getAvailableQuantity() - item.getQuantity());
            inv.setReservedQuantity(inv.getReservedQuantity() + item.getQuantity());
            inventoryRepository.save(inv);

            Reservation reservation = Reservation.builder()
                    .reservationId(UUID.randomUUID())
                    .orderId(request.getOrderId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .build();
            savedReservations.add(reservationRepository.save(reservation));
        }

        log.info("Inventory reserved successfully for orderId: {} ({} items)", request.getOrderId(), savedReservations.size());
        return buildSuccessResponse(savedReservations);
    }

    /**
     * Cancel reservation - IDEMPOTENT compensation operation
     * Called by Saga Orchestrator on failure
     */
    @Transactional
    public void cancelReservation(String orderId) {
        log.info("Cancelling reservation for orderId: {}", orderId);

        var reservations = reservationRepository.findAllByOrderId(orderId);
        if (reservations.isEmpty()) {
            log.info("No reservations found for orderId: {}, treating as idempotent success", orderId);
            return;
        }

        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                log.warn("Cannot cancel confirmed reservation for orderId: {} productId: {}", orderId, reservation.getProductId());
                continue;
            }

            Inventory inventory = inventoryRepository
                    .findByProductIdWithLock(reservation.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + reservation.getProductId()));

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
        }

        log.info("Reservations cancelled successfully for orderId: {}", orderId);
    }

    /**
     * Confirm reservation - Final step, cannot be undone
     * Called by Saga Orchestrator on success
     */
    @Transactional
    public void confirmReservation(String orderId) {
        log.info("Confirming reservation for orderId: {}", orderId);

        var reservations = reservationRepository.findAllByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new RuntimeException("Reservation not found for orderId: " + orderId);
        }

        boolean anyUpdated = false;
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                continue;
            }
            Inventory inventory = inventoryRepository
                    .findByProductIdWithLock(reservation.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + reservation.getProductId()));

            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
            anyUpdated = true;
        }

        if (anyUpdated) {
            log.info("Reservations confirmed successfully for orderId: {}", orderId);
        } else {
            log.info("Reservations already confirmed for orderId: {}", orderId);
        }
    }

    private ReserveInventoryResponse buildSuccessResponse(java.util.List<Reservation> reservations) {
        var items = reservations.stream()
                .map(r -> ReserveInventoryResponse.ItemReservation.builder()
                        .reservationId(r.getReservationId())
                        .productId(r.getProductId())
                        .quantity(r.getQuantity())
                        .status(r.getStatus())
                        .build())
                .toList();
        return ReserveInventoryResponse.builder()
                .success(true)
                .orderId(reservations.get(0).getOrderId())
                .message("Inventory reserved successfully")
                .items(items)
                .build();
    }

    public java.util.List<Reservation> getReservationsByOrderId(String orderId) {
        return reservationRepository.findAllByOrderId(orderId);
    }
}
