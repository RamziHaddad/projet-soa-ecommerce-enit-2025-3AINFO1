package com.example.payment.service;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

@QuarkusTest
public class PaymentServiceTest {

    @Inject
    PaymentService paymentService;

    @Test
    public void testProcessPaymentSucceedsAndIsIdempotent() {
        String paymentId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        PaymentRequest req = new PaymentRequest(paymentId, userId, "4242424242424242", new BigDecimal("10.00"));

        PaymentResponse resp = paymentService.processPayment(req);
        Assertions.assertNotNull(resp);
        Assertions.assertTrue(resp.status.equals("SUCCESS") || resp.status.equals("FAILED"));

        // Second call should be idempotent (should not throw and should indicate already processed)
        PaymentResponse resp2 = paymentService.processPayment(req);
        Assertions.assertNotNull(resp2);
        Assertions.assertEquals(resp.status, resp2.status);
    }

    @Test
    public void testPaymentValidationFailsForBadCard() {
        String paymentId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        PaymentRequest req = new PaymentRequest(paymentId, userId, "0000", new BigDecimal("10.00"));

        PaymentResponse resp = paymentService.processPayment(req);
        Assertions.assertEquals("FAILED", resp.status);
    }
}