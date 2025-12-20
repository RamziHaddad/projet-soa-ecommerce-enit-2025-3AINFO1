package com.example.payment.service;

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
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest request) {
        LOG.info("Processing payment for paymentId: {}", request.paymentId);

        // Idempotence check
        Paiement existing = Paiement.find("paymentId", request.getPaymentIdAsUUID()).firstResult();
        if (existing != null) {
            LOG.warn("Payment already exists for paymentId: {}", request.paymentId);
            return CompletableFuture.completedFuture(new PaymentResponse(request.paymentId, existing.status, "Payment already processed"));
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
            return CompletableFuture.completedFuture(new PaymentResponse(request.paymentId, "FAILED", "Validation failed"));
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
            return CompletableFuture.completedFuture(new PaymentResponse(request.paymentId, "SUCCESS", "Payment processed successfully"));
        } else {
            paiement.status = "FAILED";
            paiement.attempts++;
            paiement.previousStep = "PROCESS";
            paiement.nextStep = "FAILED";
            paiement.persist();
            publishEvent(paiement, "PAYMENT_FAILED");
            LOG.warn("Payment failed for paymentId: {}", request.paymentId);
            return CompletableFuture.completedFuture(new PaymentResponse(request.paymentId, "FAILED", "Payment processing failed"));
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

    private boolean simulatePayment(PaymentRequest request) {
        // Simulate 80% success rate
        return Math.random() > 0.2;
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
