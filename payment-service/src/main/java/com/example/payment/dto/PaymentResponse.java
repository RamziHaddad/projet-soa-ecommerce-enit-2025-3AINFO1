package com.example.payment.dto;

public class PaymentResponse {

    public String paymentId;
    public String status;
    public String message;

    public PaymentResponse() {}

    public PaymentResponse(String paymentId, String status, String message) {
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
    }
}
