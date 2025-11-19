package com.onlineshop.order.client;

import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.response.PaymentResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Payment Service
 */
@FeignClient(name = "payment-service", url = "${services.payment.url}")
public interface PaymentServiceClient {
    
    @PostMapping("/api/payment/process")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
    
    @PostMapping("/api/payment/refund/{transactionId}")
    PaymentResponse refundPayment(@PathVariable("transactionId") String transactionId);
}
