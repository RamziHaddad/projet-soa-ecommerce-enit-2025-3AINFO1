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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getPreviousStep() {
        return previousStep;
    }

    public void setPreviousStep(String previousStep) {
        this.previousStep = previousStep;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}
