package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByOrderId(String orderId);

    List<Reservation> findAllByOrderId(String orderId);
}
