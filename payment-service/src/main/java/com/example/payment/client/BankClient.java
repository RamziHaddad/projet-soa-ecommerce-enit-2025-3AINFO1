package com.example.payment.client;

import com.example.payment.dto.PaymentRequest;

public interface BankClient {
    /**
     * Calls the external bank API to process a payment.
     * Returns true if bank reports success, false otherwise.
     */
    boolean processPayment(PaymentRequest request);
}
