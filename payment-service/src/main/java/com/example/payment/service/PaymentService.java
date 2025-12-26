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
                    existing.getStatus(),
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

        paiement.setStatus("PENDING");
        paiement.setPreviousStep("INIT");
        paiement.setNextStep("VALIDATE");

        // Validate payment
        if (!validatePayment(request)) {
            paiement.setStatus("FAILED");
            paiement.setAttempts(paiement.getAttempts() + 1);
            paiement.setPreviousStep("VALIDATE");
            paiement.setNextStep("FAILED");
            paiement.persist();

            publishEvent(paiement, "PAYMENT_FAILED");

            return new PaymentResponse(
                    request.paymentId(),
                    "FAILED",
                    "Validation failed"
            );
        }

        paiement.setPreviousStep("VALIDATE");
        paiement.setNextStep("PROCESS");

        // Call external bank synchronously
        boolean success = bankClient.processPayment(request);

        if (success) {
            paiement.setStatus("SUCCESS");
            paiement.setPreviousStep("PROCESS");
            paiement.setNextStep("COMPLETED");
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
        paiement.setStatus("FAILED");
        paiement.setAttempts(paiement.getAttempts() + 1);
        paiement.setPreviousStep("PROCESS");
        paiement.setNextStep("FAILED");
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
    // REFUND PAYMENT
    // -------------------------------
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement == null) {
            LOG.warn("Payment not found for paymentId: {}", paymentId);
            return new PaymentResponse(paymentId.toString(), "NOT_FOUND", "Payment does not exist");
        }

        if (!"SUCCESS".equals(paiement.getStatus())) {
            LOG.warn("Cannot refund payment with status: {}", paiement.getStatus());
            return new PaymentResponse(paymentId.toString(), paiement.getStatus(),
                    "Cannot refund payment with status: " + paiement.getStatus());
        }

        // Call external bank API to refund
        boolean refunded;
        try {
            refunded = bankClient.refundPayment(paiement.getPaymentId(), paiement.getAmount());
        } catch (Exception e) {
            LOG.error("Bank refund failed for paymentId {}", paymentId, e);
            return new PaymentResponse(paymentId.toString(), "FAILED", "Refund failed due to bank error");
        }

        if (refunded) {
            paiement.setStatus("REFUNDED");
            paiement.setPreviousStep(paiement.getNextStep());
            paiement.setNextStep("REFUNDED");
            paiement.persist();

            publishEvent(paiement, "PAYMENT_REFUNDED");

            LOG.info("Payment refunded successfully for paymentId: {}", paymentId);
            return new PaymentResponse(paymentId.toString(), "REFUNDED", "Payment refunded successfully");
        } else {
            LOG.warn("Bank refund failed for paymentId {}", paymentId);
            return new PaymentResponse(paymentId.toString(), "FAILED", "Refund failed");
        }
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
    // QUERIES
    // -------------------------------
    public PaymentResponse getPaymentStatus(UUID paymentId) {

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement == null) {
            LOG.warn("No payment found for paymentId: {}", paymentId);
            return null;
        }

        String message = switch (paiement.getStatus()) {
            case "SUCCESS" -> "Payment completed successfully";
            case "FAILED" -> "Payment failed";
            case "PENDING" -> "Payment is being processed";
            default -> "Payment status: " + paiement.getStatus();
        };

        return new PaymentResponse(
                paiement.getPaymentId().toString(),
                paiement.getStatus(),
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
                paiement.getPaymentId(),
                paiement.getUserId(),
                paiement.getCardNumber(),
                paiement.getAmount(),
                paiement.getStatus(),
                paiement.getAttempts(),
                paiement.getPreviousStep(),
                paiement.getNextStep(),
                paiement.getCreatedAt()
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

        if (!"PENDING".equals(paiement.getStatus())) {
            return new PaymentResponse(
                    paymentId.toString(),
                    paiement.getStatus(),
                    "Cannot cancel payment with status: " + paiement.getStatus()
            );
        }

        // Optionally notify bank
        boolean cancelled = bankClient.cancelPayment(paymentId);
        if (!cancelled) {
            LOG.warn("Bank did not cancel payment, proceeding locally for paymentId {}", paymentId);
        }

        paiement.setStatus("CANCELLED");
        paiement.setPreviousStep(paiement.getNextStep());
        paiement.setNextStep("CANCELLED");
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
                paiement.getPaymentId(),
                paiement.getUserId(),
                paiement.getAmount(),
                paiement.getStatus()
        );

        LOG.info("Sending notification [{}]: {}", eventType, payload);

        boolean sent = notificationClient.sendNotification(payload);
        if (!sent) {
            LOG.warn("Notification delivery failed for paymentId: {}", paiement.getPaymentId());
        }
    }
}
