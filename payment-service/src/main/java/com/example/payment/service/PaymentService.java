package com.example.payment.service;

import com.example.payment.dto.PaymentDetails;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.Paiement;
import com.example.payment.client.NotificationClient;
import com.example.payment.client.BankClient;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    @Inject
    NotificationClient notificationClient;

    @Inject
    BankClient bankClient;

    // -------------------------------
    // PROCESS PAYMENT (SYNCHRONOUS)
    // -------------------------------
    @Transactional
    @CircuitBreaker
    @Retry
    @Timeout
    public PaymentResponse processPayment(PaymentRequest request) {

        LOG.info("Processing payment for paymentId: {}", request.paymentId());

        // Idempotence check
        Paiement existing = Paiement.find("paymentId", request.getPaymentIdAsUUID()).firstResult();
        if (existing != null) {
            LOG.warn("Payment already exists for paymentId: {}", request.paymentId());
            return new PaymentResponse(
                    request.paymentId(),
                    existing.status,
                    "Payment already processed"
            );
        }

        // Create payment entity
        Paiement paiement = new Paiement(
                request.getPaymentIdAsUUID(),
                request.getUserIdAsUUID(),
                request.cardNumber(),
                request.amount()
        );

        paiement.status = "PENDING";
        paiement.previousStep = "INIT";
        paiement.nextStep = "VALIDATE";

        // Validate payment
        if (!validatePayment(request)) {
            paiement.status = "FAILED";
            paiement.attempts++;
            paiement.previousStep = "VALIDATE";
            paiement.nextStep = "FAILED";
            paiement.persist();

            publishEvent(paiement, "PAYMENT_FAILED");

            return new PaymentResponse(
                    request.paymentId(),
                    "FAILED",
                    "Validation failed"
            );
        }

        paiement.previousStep = "VALIDATE";
        paiement.nextStep = "PROCESS";

        // Call bank synchronously
        boolean success = simulatePayment(request);

        if (success) {
            paiement.status = "SUCCESS";
            paiement.previousStep = "PROCESS";
            paiement.nextStep = "COMPLETED";
            paiement.persist();

            publishEvent(paiement, "PAYMENT_SUCCESS");

            LOG.info("Payment successful for paymentId: {}", request.paymentId());

            return new PaymentResponse(
                    request.paymentId(),
                    "SUCCESS",
                    "Payment processed successfully"
            );
        }

        // Failure case
        paiement.status = "FAILED";
        paiement.attempts++;
        paiement.previousStep = "PROCESS";
        paiement.nextStep = "FAILED";
        paiement.persist();

        publishEvent(paiement, "PAYMENT_FAILED");

        LOG.warn("Payment failed for paymentId: {}", request.paymentId());

        return new PaymentResponse(
                request.paymentId(),
                "FAILED",
                "Payment processing failed"
        );
    }

    // -------------------------------
    // VALIDATION
    // -------------------------------
    private boolean validatePayment(PaymentRequest request) {

        if (request.userId() == null ||
                request.cardNumber() == null ||
                request.amount() == null) {
            return false;
        }

        if (!request.cardNumber().matches("\\d{16}")) {
            return false;
        }

        return request.amount().compareTo(BigDecimal.ZERO) > 0;
    }

    // -------------------------------
    // BANK CALL (BLOCKING)
    // -------------------------------
    private boolean simulatePayment(PaymentRequest request) {
        try {
            boolean bankSuccess = bankClient.processPayment(request);
            LOG.info("Bank result for paymentId {}: {}", request.paymentId(), bankSuccess);
            return bankSuccess;
        } catch (Exception e) {
            LOG.warn("Bank client failed, falling back to local simulation", e);
            return Math.random() > 0.2;
        }
    }

    // -------------------------------
    // QUERIES
    // -------------------------------
    public PaymentResponse getPaymentStatus(UUID paymentId) {

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement == null) {
            LOG.warn("No payment found for paymentId: {}", paymentId);
            return null;
        }

        String message = switch (paiement.status) {
            case "SUCCESS" -> "Payment completed successfully";
            case "FAILED" -> "Payment failed";
            case "PENDING" -> "Payment is being processed";
            default -> "Payment status: " + paiement.status;
        };

        return new PaymentResponse(
                paiement.paymentId.toString(),
                paiement.status,
                message
        );
    }

    public List<PaymentResponse> getPaymentsByUser(UUID userId) {

        PanacheQuery<Paiement> query = Paiement.find("userId", userId);

        List<Paiement> paiements = query.list(); // now strongly typed

        return paiements.stream()
                .map(p -> new PaymentResponse(
                        p.getPaymentId().toString(),
                        p.getStatus(),
                        "Payment status: " + p.getStatus()
                ))
                .collect(Collectors.toList());
    }

    public PaymentDetails getPaymentDetails(UUID paymentId) {

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement == null) {
            return null;
        }

        return new PaymentDetails(
                paiement.paymentId,
                paiement.userId,
                paiement.cardNumber,
                paiement.amount,
                paiement.status,
                paiement.attempts,
                paiement.previousStep,
                paiement.nextStep,
                paiement.createdAt
        );
    }

    // -------------------------------
    // CANCEL PAYMENT
    // -------------------------------
    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId) {

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement == null) {
            return null;
        }

        if (!"PENDING".equals(paiement.status)) {
            return new PaymentResponse(
                    paymentId.toString(),
                    paiement.status,
                    "Cannot cancel payment with status: " + paiement.status
            );
        }

        paiement.status = "CANCELLED";
        paiement.previousStep = paiement.nextStep;
        paiement.nextStep = "CANCELLED";
        paiement.persist();

        publishEvent(paiement, "PAYMENT_CANCELLED");

        return new PaymentResponse(
                paymentId.toString(),
                "CANCELLED",
                "Payment cancelled successfully"
        );
    }

    // -------------------------------
    // WEBHOOK (ACK ONLY)
    // -------------------------------
    public String processWebhook(String payload) {
        LOG.info("Webhook received: {}", payload);
        return "Webhook processed successfully";
    }

    // -------------------------------
    // SYNCHRONOUS NOTIFICATION
    // -------------------------------
    private void publishEvent(Paiement paiement, String eventType) {

        String payload = String.format(
                "{\"paymentId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,\"status\":\"%s\"}",
                paiement.paymentId,
                paiement.userId,
                paiement.amount,
                paiement.status
        );

        LOG.info("Sending notification [{}]: {}", eventType, payload);

        boolean sent = notificationClient.sendNotification(payload);
        if (!sent) {
            LOG.warn("Notification delivery failed for paymentId: {}", paiement.paymentId);
        }
    }
}
