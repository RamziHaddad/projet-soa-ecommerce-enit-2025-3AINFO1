package com.ecommerce.inventory.service;

import com.ecommerce.inventory.InventoryServiceApplication;
import com.ecommerce.inventory.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.dto.ReserveInventoryResponse;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.Reservation;
import com.ecommerce.inventory.model.ReservationStatus;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = InventoryServiceApplication.class)
@ActiveProfiles("test")
public class InventoryServiceMultiItemTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate; // prevent real Kafka usage

    @BeforeEach
    void resetData() {
        reservationRepository.deleteAll();
        inventoryRepository.findAll().forEach(inv -> {
            inv.setReservedQuantity(0);
            // Reset available to baseline for PROD-001 and PROD-002 for deterministic tests
            if (inv.getProductId().equals("PROD-001")) inv.setAvailableQuantity(100);
            if (inv.getProductId().equals("PROD-002")) inv.setAvailableQuantity(50);
            inventoryRepository.save(inv);
        });
    }

    @Test
    void reserveMultipleItems_allAppliedAtomically_andIdempotent() {
        var request = ReserveInventoryRequest.builder()
                .orderId("ORD-MULTI-1")
                .items(List.of(
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-001").quantity(3).build(),
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-002").quantity(4).build()
                ))
                .build();

        ReserveInventoryResponse resp1 = inventoryService.reserveInventory(request);
        assertThat(resp1.isSuccess()).isTrue();
        assertThat(resp1.getItems()).hasSize(2);

        Inventory i1 = inventoryRepository.findByProductId("PROD-001").orElseThrow();
        Inventory i2 = inventoryRepository.findByProductId("PROD-002").orElseThrow();
        assertThat(i1.getAvailableQuantity()).isEqualTo(97);
        assertThat(i1.getReservedQuantity()).isEqualTo(3);
        assertThat(i2.getAvailableQuantity()).isEqualTo(46);
        assertThat(i2.getReservedQuantity()).isEqualTo(4);

        // Idempotent repeat returns same reservations and does not double-reserve
        ReserveInventoryResponse resp2 = inventoryService.reserveInventory(request);
        assertThat(resp2.getItems()).hasSize(2);
        assertThat(inventoryRepository.findByProductId("PROD-001").orElseThrow().getReservedQuantity()).isEqualTo(3);
        assertThat(inventoryRepository.findByProductId("PROD-002").orElseThrow().getReservedQuantity()).isEqualTo(4);
    }

    @Test
    void cancelReservation_restoresCounters_forAllItems() {
        var request = ReserveInventoryRequest.builder()
                .orderId("ORD-MULTI-2")
                .items(List.of(
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-001").quantity(2).build(),
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-002").quantity(2).build()
                ))
                .build();
        inventoryService.reserveInventory(request);
        inventoryService.cancelReservation("ORD-MULTI-2");

        Inventory i1 = inventoryRepository.findByProductId("PROD-001").orElseThrow();
        Inventory i2 = inventoryRepository.findByProductId("PROD-002").orElseThrow();
        assertThat(i1.getAvailableQuantity()).isEqualTo(100);
        assertThat(i1.getReservedQuantity()).isZero();
        assertThat(i2.getAvailableQuantity()).isEqualTo(50);
        assertThat(i2.getReservedQuantity()).isZero();

        List<Reservation> reservations = reservationRepository.findAllByOrderId("ORD-MULTI-2");
        assertThat(reservations).allMatch(r -> r.getStatus() == ReservationStatus.CANCELLED);
    }

    @Test
    void confirmReservation_movesFromReservedToSold_forAllItems() {
        var request = ReserveInventoryRequest.builder()
                .orderId("ORD-MULTI-3")
                .items(List.of(
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-001").quantity(5).build(),
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-002").quantity(1).build()
                ))
                .build();
        inventoryService.reserveInventory(request);
        inventoryService.confirmReservation("ORD-MULTI-3");

        Inventory i1 = inventoryRepository.findByProductId("PROD-001").orElseThrow();
        Inventory i2 = inventoryRepository.findByProductId("PROD-002").orElseThrow();
        assertThat(i1.getReservedQuantity()).isZero();
        assertThat(i2.getReservedQuantity()).isZero();

        List<Reservation> reservations = reservationRepository.findAllByOrderId("ORD-MULTI-3");
        assertThat(reservations).allMatch(r -> r.getStatus() == ReservationStatus.CONFIRMED);
    }

    @Test
    void insufficientStock_failsWholeReservation() {
        var request = ReserveInventoryRequest.builder()
                .orderId("ORD-MULTI-4")
                .items(List.of(
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-001").quantity(3).build(),
                        // Exceed PROD-002 availability 50
                        ReserveInventoryRequest.ItemRequest.builder().productId("PROD-002").quantity(999).build()
                ))
                .build();
        assertThatThrownBy(() -> inventoryService.reserveInventory(request))
                .isInstanceOf(InsufficientStockException.class);

        // Ensure nothing reserved for either item
        Inventory i1 = inventoryRepository.findByProductId("PROD-001").orElseThrow();
        Inventory i2 = inventoryRepository.findByProductId("PROD-002").orElseThrow();
        assertThat(i1.getReservedQuantity()).isZero();
        assertThat(i2.getReservedQuantity()).isZero();
    }
}
