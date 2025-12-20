package com.example.payment.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paiements")
public class Paiement extends PanacheEntityBase {

    @Id
    @Column(name = "payment_id")
    public UUID paymentId;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "card_number", nullable = false, length = 16)
    public String cardNumber;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    public BigDecimal amount;

    @Column(name = "status", nullable = false, length = 10)
    public String status;

    @Column(name = "attempts")
    public int attempts = 0;

    @Column(name = "previous_step", length = 50)
    public String previousStep;

    @Column(name = "next_step", length = 50)
    public String nextStep;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Paiement() {}

    public Paiement(UUID paymentId, UUID userId, String cardNumber, BigDecimal amount) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.status = "PENDING";
        this.previousStep = "INIT";
        this.nextStep = "VALIDATE";
    }
}
