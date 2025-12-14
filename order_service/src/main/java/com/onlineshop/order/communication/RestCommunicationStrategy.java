package com.onlineshop.order.communication;

import org.springframework.stereotype.Component;

import com.onlineshop.order.client.InventoryServiceClient;
import com.onlineshop.order.client.PaymentServiceClient;
import com.onlineshop.order.client.ShippingServiceClient;
import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.InventoryResponse;
import com.onlineshop.order.dto.response.PaymentResponse;
import com.onlineshop.order.dto.response.ShippingResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * REST-based implementation of CommunicationStrategy using Feign clients
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestCommunicationStrategy implements CommunicationStrategy {

    private final InventoryServiceClient inventoryClient;
    private final PaymentServiceClient paymentClient;
    private final ShippingServiceClient shippingClient;

    @Override
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackReserveInventory")
    @Retry(name = "inventoryService")
    public InventoryResponse reserveInventory(InventoryRequest request) {
        log.info("Reserving inventory for order: {}", request.getOrderNumber());
        
        try {
            var response = inventoryClient.reserveInventory(request);
            log.info("Inventory reservation response for order {}: {}", 
                request.getOrderNumber(), response);
            return response;
        } catch (Exception e) {
            log.error("Failed to reserve inventory for order: {}", request.getOrderNumber(), e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackReleaseInventory")
    @Retry(name = "inventoryService")
    public InventoryResponse releaseInventory(String transactionId) {
        log.info("Releasing inventory for transaction: {}", transactionId);
        
        try {
            var response = inventoryClient.releaseInventory(transactionId);
            log.info("Inventory release response for transaction {}: {}", transactionId, response);
            return response;
        } catch (Exception e) {
            log.error("Failed to release inventory for transaction: {}", transactionId, e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackProcessPayment")
    @Retry(name = "paymentService")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderNumber());
        
        try {
            var response = paymentClient.processPayment(request);
            log.info("Payment processing response for order {}: {}", 
                request.getOrderNumber(), response);
            return response;
        } catch (Exception e) {
            log.error("Failed to process payment for order: {}", request.getOrderNumber(), e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackRefundPayment")
    @Retry(name = "paymentService")
    public PaymentResponse refundPayment(String transactionId) {
        log.info("Refunding payment for transaction: {}", transactionId);
        
        try {
            var response = paymentClient.refundPayment(transactionId);
            log.info("Payment refund response for transaction {}: {}", transactionId, response);
            return response;
        } catch (Exception e) {
            log.error("Failed to refund payment for transaction: {}", transactionId, e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "shippingService", fallbackMethod = "fallbackArrangeShipping")
    @Retry(name = "shippingService")
    public ShippingResponse arrangeShipping(ShippingRequest request) {
        log.info("Arranging shipping for order: {}", request.getOrderNumber());
        
        try {
            var response = shippingClient.arrangeShipping(request);
            log.info("Shipping arrangement response for order {}: {}", 
                request.getOrderNumber(), response);
            return response;
        } catch (Exception e) {
            log.error("Failed to arrange shipping for order: {}", request.getOrderNumber(), e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "shippingService", fallbackMethod = "fallbackCancelShipping")
    @Retry(name = "shippingService")
    public ShippingResponse cancelShipping(String trackingNumber) {
        log.info("Cancelling shipping for tracking number: {}", trackingNumber);
        
        try {
            var response = shippingClient.cancelShipping(trackingNumber);
            log.info("Shipping cancellation response for tracking {}: {}", trackingNumber, response);
            return response;
        } catch (Exception e) {
            log.error("Failed to cancel shipping for tracking: {}", trackingNumber, e);
            throw e;
        }
    }
    
    // Fallback methods for circuit breaker
    public InventoryResponse fallbackReserveInventory(InventoryRequest request, Exception ex) {
        log.warn("Circuit breaker triggered for inventory reservation. Order: {}", request.getOrderNumber());
        return InventoryResponse.builder()
                .success(false)
                .message("Service temporarily unavailable: " + ex.getMessage())
                .build();
    }
    
    public InventoryResponse fallbackReleaseInventory(String transactionId, Exception ex) {
        log.warn("Circuit breaker triggered for inventory release. Transaction: {}", transactionId);
        return InventoryResponse.builder()
                .success(false)
                .message("Service temporarily unavailable: " + ex.getMessage())
                .build();
    }
    
    public PaymentResponse fallbackProcessPayment(PaymentRequest request, Exception ex) {
        log.warn("Circuit breaker triggered for payment processing. Order: {}", request.getOrderNumber());
        return PaymentResponse.builder()
                .success(false)
                .message("Service temporarily unavailable: " + ex.getMessage())
                .build();
    }
    
    public PaymentResponse fallbackRefundPayment(String transactionId, Exception ex) {
        log.warn("Circuit breaker triggered for payment refund. Transaction: {}", transactionId);
        return PaymentResponse.builder()
                .success(false)
                .message("Service temporarily unavailable: " + ex.getMessage())
                .build();
    }
    
    public ShippingResponse fallbackArrangeShipping(ShippingRequest request, Exception ex) {
        log.warn("Circuit breaker triggered for shipping arrangement. Order: {}", request.getOrderNumber());
        return ShippingResponse.builder()
                .success(false)
                .message("Service temporarily unavailable: " + ex.getMessage())
                .build();
    }
    
    public ShippingResponse fallbackCancelShipping(String trackingNumber, Exception ex) {
        log.warn("Circuit breaker triggered for shipping cancellation. Tracking: {}", trackingNumber);
        return ShippingResponse.builder()
                .success(false)
                .message("Service temporarily unavailable: " + ex.getMessage())
                .build();
    }
}
