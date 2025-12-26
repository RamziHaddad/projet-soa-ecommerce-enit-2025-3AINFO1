package com.example.payment.client;

import com.example.payment.dto.PaymentRequest;
import java.math.BigDecimal;
import java.util.UUID;

public interface BankClient {

    /**
     * Calls the external bank API to process a payment.
     * Returns true if bank reports success, false otherwise.
     */
    boolean processPayment(PaymentRequest request);

    /**
     * Calls the external bank API to refund a payment.
     */
    boolean refundPayment(UUID paymentId, BigDecimal amount);

    /**
     * Optional: call bank to cancel a pending payment.
     */
    boolean cancelPayment(UUID paymentId);
}
