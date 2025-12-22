package com.example.payment.service;

import com.example.payment.dto.PaymentDetails;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.Outbox;
import com.example.payment.entity.Paiement;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.enterprise.inject.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    @Inject
    @Channel("payment-events")
    Instance<Emitter<String>> eventEmitterInstance;

    @Inject
    SagaService sagaService;

    @Transactional
    @CircuitBreaker
    @Retry
    @Timeout
    public PaymentResponse processPayment(PaymentRequest request) {
        LOG.info("Processing payment for paymentId: {}", request.paymentId);

        // Idempotence check
        Paiement existing = Paiement.find("paymentId", request.getPaymentIdAsUUID()).firstResult();
        if (existing != null) {
            LOG.warn("Payment already exists for paymentId: {}", request.paymentId);
            return new PaymentResponse(request.paymentId, existing.status, "Payment already processed");
        }

        // Create payment entity
        Paiement paiement = new Paiement(request.getPaymentIdAsUUID(), request.getUserIdAsUUID(), request.cardNumber, request.amount);
        paiement.previousStep = "INIT";
        paiement.nextStep = "VALIDATE";

        // Validate
        if (!validatePayment(request)) {
            paiement.status = "FAILED";
            paiement.attempts++;
            paiement.previousStep = "VALIDATE";
            paiement.nextStep = "FAILED";
            paiement.persist();
            publishEvent(paiement, "PAYMENT_FAILED");
            return new PaymentResponse(request.paymentId, "FAILED", "Validation failed");
        }

        paiement.previousStep = "VALIDATE";
        paiement.nextStep = "PROCESS";

        // Simulate payment processing
        boolean success = simulatePayment(request);

        if (success) {
            paiement.status = "SUCCESS";
            paiement.previousStep = "PROCESS";
            paiement.nextStep = "COMPLETED";
            paiement.persist();
            // Start saga for coordination
            sagaService.startPaymentSaga(paiement);
            publishEvent(paiement, "PAYMENT_SUCCESS");
            LOG.info("Payment successful for paymentId: {}", request.paymentId);
            return new PaymentResponse(request.paymentId, "SUCCESS", "Payment processed successfully");
        } else {
            paiement.status = "FAILED";
            paiement.attempts++;
            paiement.previousStep = "PROCESS";
            paiement.nextStep = "FAILED";
            paiement.persist();
            publishEvent(paiement, "PAYMENT_FAILED");
            LOG.warn("Payment failed for paymentId: {}", request.paymentId);
            return new PaymentResponse(request.paymentId, "FAILED", "Payment processing failed");
        }
    }

    private boolean validatePayment(PaymentRequest request) {
        // Basic validation
        if (request.userId == null || request.cardNumber == null || request.amount == null) {
            return false;
        }
        if (request.cardNumber.length() != 16 || !request.cardNumber.matches("\\d+")) {
            return false;
        }
        if (request.amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return true;
    }

    @jakarta.inject.Inject
    com.example.payment.client.BankClient bankClient;

    private boolean simulatePayment(PaymentRequest request) {
        // Call external bank API via BankClient. If the call fails, fall back to local stochastic simulation.
        try {
            boolean bankSuccess = bankClient.processPayment(request);
            LOG.info("Bank processing result for paymentId {}: {}", request.paymentId, bankSuccess);
            return bankSuccess;
        } catch (Exception e) {
            LOG.warn("Bank client failed, falling back to local simulation", e);
            // Fallback: 80% success
            return Math.random() > 0.2;
        }
    }

    public PaymentResponse getPaymentStatus(UUID paymentId) {
        LOG.info("Retrieving payment status for paymentId: {}", paymentId);

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement != null) {
            String message = switch (paiement.status) {
                case "SUCCESS" -> "Payment completed successfully";
                case "FAILED" -> "Payment failed";
                case "PENDING" -> "Payment is being processed";
                default -> "Payment status: " + paiement.status;
            };

            return new PaymentResponse(paiement.paymentId.toString(), paiement.status, message);
        }

        LOG.warn("No payment found for paymentId: {}", paymentId);
        return null;
    }

    public java.util.List<PaymentResponse> getPaymentsByUser(UUID userId) {
        LOG.info("Retrieving payments for userId: {}", userId);

        java.util.List<Paiement> paiements = Paiement.find("userId", userId).list();

        return paiements.stream()
            .map(paiement -> {
                String message = switch (paiement.status) {
                    case "SUCCESS" -> "Payment completed successfully";
                    case "FAILED" -> "Payment failed";
                    case "PENDING" -> "Payment is being processed";
                    default -> "Payment status: " + paiement.status;
                };
                return new PaymentResponse(paiement.paymentId.toString(), paiement.status, message);
            })
            .collect(java.util.stream.Collectors.toList());
    }

    public PaymentDetails getPaymentDetails(UUID paymentId) {
        LOG.info("Retrieving payment details for paymentId: {}", paymentId);

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement != null) {
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

        LOG.warn("No payment found for paymentId: {}", paymentId);
        return null;
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId) {
        LOG.info("Attempting to cancel payment for paymentId: {}", paymentId);

        Paiement paiement = Paiement.find("paymentId", paymentId).firstResult();

        if (paiement == null) {
            LOG.warn("Payment not found for paymentId: {}", paymentId);
            return null;
        }

        // Only allow cancellation for pending payments
        if ("PENDING".equals(paiement.status)) {
            paiement.status = "CANCELLED";
            paiement.previousStep = paiement.nextStep;
            paiement.nextStep = "CANCELLED";
            paiement.persist();

            publishEvent(paiement, "PAYMENT_CANCELLED");
            LOG.info("Payment cancelled for paymentId: {}", paymentId);
            return new PaymentResponse(paymentId.toString(), "CANCELLED", "Payment cancelled successfully");
        } else {
            LOG.warn("Cannot cancel payment with status: {} for paymentId: {}", paiement.status, paymentId);
            return new PaymentResponse(paymentId.toString(), paiement.status,
                "Cannot cancel payment with status: " + paiement.status);
        }
    }

    public String processWebhook(String payload) {
        LOG.info("Processing webhook payload: {}", payload);

        try {
            // Parse webhook payload (simplified - in real implementation, use JSON parser)
            // For now, just acknowledge receipt
            LOG.info("Webhook processed successfully: {}", payload);
            return "Webhook processed successfully";
        } catch (Exception e) {
            LOG.error("Error processing webhook payload", e);
            throw new RuntimeException("Failed to process webhook", e);
        }
    }

    private void publishEvent(Paiement paiement, String eventType) {
        String payload = String.format("{\"paymentId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,\"status\":\"%s\"}",
                paiement.paymentId, paiement.userId, paiement.amount, paiement.status);

        // For testing, just log the event instead of using outbox
        LOG.info("Publishing event: {} - {}", eventType, payload);

        // Publish to Kafka if emitter is available
        if (eventEmitterInstance.isResolvable()) {
            Emitter<String> eventEmitter = eventEmitterInstance.get();
            eventEmitter.send(payload).toCompletableFuture().exceptionally(throwable -> {
                LOG.error("Failed to send event to Kafka", throwable);
                return null;
            });
        }
    }
}
