package com.onlineshop.order.communication;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.onlineshop.order.client.InventoryServiceClient;
import com.onlineshop.order.client.PaymentServiceClient;
import com.onlineshop.order.client.ShippingServiceClient;
import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.DeliveryResponse;
import com.onlineshop.order.dto.response.InventoryResponse;
import com.onlineshop.order.dto.response.PaymentResponse;
import com.onlineshop.order.dto.response.ShippingResponse;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST-based implementation of CommunicationStrategy using Feign clients.
 * Handles resilience patterns (Retry, Bulkhead, CircuitBreaker) and returns
 * structured responses for SAGA orchestration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestCommunicationStrategy implements OrderProcessingCommunicationHandler {

    private final InventoryServiceClient inventoryClient;
    private final PaymentServiceClient paymentClient;
    private final ShippingServiceClient shippingClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // ====== INVENTORY OPERATIONS ======

    @Override
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackReserveInventory")
    @Retry(name = "inventoryService")
    @Bulkhead(name = "inventoryService")
    public InventoryResponse reserveInventory(InventoryRequest request) {
        log.info("Reserving inventory for order: {}", request.orderId());

        try {
            var response = inventoryClient.reserveInventory(request);
            log.info("Inventory reservation response for order {}: {}", request.orderId(), response);
            return response;
        } catch (RequestNotPermitted | BulkheadFullException e) {
            log.warn("Inventory service rejected due to rate/bulkhead limit for order: {}", request.orderId(),
                    e);
            return createOverloadInventoryResponse("Inventory service busy - please retry later");
        } catch (Exception e) {
            log.error("Unexpected error during inventory reservation for order: {}", request.orderId(), e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackReleaseInventory")
    @Retry(name = "inventoryService")
    @Bulkhead(name = "inventoryService")
    public InventoryResponse releaseInventory(String orderId) {
        log.info("Cancelling inventory reservation for order: {}", orderId);

        try {
            inventoryClient.cancelReservation(orderId);
            log.info("Inventory reservation cancelled for order: {}", orderId);
            return new InventoryResponse(true, orderId, "Inventory reservation cancelled successfully", List.of());
        } catch (RequestNotPermitted | BulkheadFullException e) {
            log.warn("Inventory cancellation rejected due to rate/bulkhead limit for order: {}", orderId, e);
            return createOverloadInventoryResponse("Failed to cancel inventory reservation – system busy, will retry");
        } catch (Exception e) {
            log.error("Unexpected error during inventory cancellation for order: {}", orderId, e);
            throw e;
        }
    }

    // ====== PAYMENT OPERATIONS ======

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackProcessPayment")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.orderNumber());

        try {
            var response = paymentClient.processPayment(request);
            log.info("Payment processing response for order {}: {}", request.orderNumber(), response);
            return response;
        } catch (RequestNotPermitted | BulkheadFullException e) {
            log.warn("Payment service rejected due to rate/bulkhead limit for order: {}", request.orderNumber(), e);
            return createOverloadPaymentResponse("Payment service busy - please retry later");
        } catch (Exception e) {
            log.error("Unexpected error during payment processing for order: {}", request.orderNumber(), e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackRefundPayment")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public PaymentResponse refundPayment(String transactionId) {
        log.info("Refunding payment for transaction: {}", transactionId);

        try {
            var response = paymentClient.refundPayment(transactionId);
            log.info("Payment refund response for transaction {}: {}", transactionId, response);
            return response;
        } catch (RequestNotPermitted | BulkheadFullException e) {
            log.warn("Payment refund rejected due to rate/bulkhead limit for transaction: {}", transactionId, e);
            return createOverloadPaymentResponse("Failed to refund payment – system busy, will retry");
        } catch (Exception e) {
            log.error("Unexpected error during payment refund for transaction: {}", transactionId, e);
            throw e;
        }
    }

    // ====== SHIPPING OPERATIONS ======

    @Override
    @CircuitBreaker(name = "shippingService", fallbackMethod = "fallbackArrangeShipping")
    @Retry(name = "shippingService")
    @Bulkhead(name = "shippingService")
    public ShippingResponse arrangeShipping(ShippingRequest request) {
        log.info("Arranging shipping for order: {}", request.orderNumber());

        try {
            var response = shippingClient.arrangeShipping(request);
            log.info("Shipping arrangement response for order {}: {}", request.orderNumber(), response);
            return shippingResponseMapper(response);
        } catch (RequestNotPermitted | BulkheadFullException e) {
            log.warn("Shipping service rejected due to rate/bulkhead limit for order: {}", request.orderNumber(), e);
            return createOverloadShippingResponse("Shipping service busy - please retry later");
        } catch (Exception e) {
            log.error("Unexpected error during shipping arrangement for order: {}", request.orderNumber(), e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "shippingService", fallbackMethod = "fallbackCancelShipping")
    @Retry(name = "shippingService")
    @Bulkhead(name = "shippingService")
    public ShippingResponse cancelShipping(String trackingNumber) {
        log.info("Cancelling shipping for tracking number: {}", trackingNumber);

        try {
            var response = shippingClient.cancelShipping(trackingNumber);
            log.info("Shipping cancellation response for tracking {}: {}", trackingNumber, response);
            return response;
        } catch (RequestNotPermitted | BulkheadFullException e) {
            log.warn("Shipping cancellation rejected due to rate/bulkhead limit for tracking: {}", trackingNumber, e);
            return createOverloadShippingResponse("Failed to cancel shipping – system busy, will retry");
        } catch (Exception e) {
            log.error("Unexpected error during shipping cancellation for tracking: {}", trackingNumber, e);
            throw e;
        }
    }

    // ====== FALLBACK METHODS ======

    public InventoryResponse fallbackReserveInventory(InventoryRequest request, Exception ex) {
        String metrics = getCircuitBreakerMetrics("inventoryService");
        log.warn("Circuit breaker triggered for inventory reservation. Order: {}. Metrics: {}",
                request.orderId(), metrics);

        boolean retryable = shouldRetryLater(ex);
        String message = retryable
                ? "Inventory service temporarily unavailable - please retry later"
                : "Inventory service failed: " + ex.getMessage();

        return new InventoryResponse(false, null, message, List.of());
    }

    public InventoryResponse fallbackReleaseInventory(String transactionId, Exception ex) {
        log.warn("Circuit breaker triggered for inventory release. Transaction: {}. Will retry. Error: {}",
                transactionId, ex.getMessage());
        return new InventoryResponse(false, null, "Failed to release inventory – will retry compensation", List.of());
    }

    public PaymentResponse fallbackProcessPayment(PaymentRequest request, Exception ex) {
        String metrics = getCircuitBreakerMetrics("paymentService");
        log.warn("Circuit breaker triggered for payment processing. Order: {}. Metrics: {}", request.orderNumber(),
                metrics);

        boolean retryable = shouldRetryLater(ex);
        String message = retryable
                ? "Payment service temporarily unavailable - please retry later"
                : "Payment processing failed: " + ex.getMessage();

        return new PaymentResponse(false, null, message, retryable, LocalDateTime.now());
    }

    public PaymentResponse fallbackRefundPayment(String transactionId, Exception ex) {
        log.warn("Circuit breaker triggered for payment refund. Transaction: {}. Will retry. Error: {}",
                transactionId, ex.getMessage());
        return new PaymentResponse(false, null, "Failed to refund payment – will retry compensation", true,
                LocalDateTime.now());
    }

    public ShippingResponse fallbackArrangeShipping(ShippingRequest request, Exception ex) {
        String metrics = getCircuitBreakerMetrics("shippingService");
        log.warn("Circuit breaker triggered for shipping arrangement. Order: {}. Metrics: {}", request.orderNumber(),
                metrics);

        boolean retryable = shouldRetryLater(ex);
        String message = retryable
                ? "Shipping service temporarily unavailable - please retry later"
                : "Shipping arrangement failed: " + ex.getMessage();

        return new ShippingResponse(false, null, message, retryable, LocalDateTime.now());
    }

    public ShippingResponse fallbackCancelShipping(String trackingNumber, Exception ex) {
        log.warn("Circuit breaker triggered for shipping cancellation. Tracking: {}. Will retry. Error: {}",
                trackingNumber, ex.getMessage());
        return new ShippingResponse(false, null, "Failed to cancel shipping – will retry compensation", true,
                LocalDateTime.now());
    }

    // ====== HELPERS ======

    private InventoryResponse createOverloadInventoryResponse(String message) {
        return new InventoryResponse(false, null, message, List.of());
    }

    private PaymentResponse createOverloadPaymentResponse(String message) {
        return new PaymentResponse(false, null, message, true, LocalDateTime.now());
    }

    private ShippingResponse createOverloadShippingResponse(String message) {
        return new ShippingResponse(false, null, message, true, LocalDateTime.now());
    }

    private String getCircuitBreakerMetrics(String circuitBreakerName) {
        try {
            var circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
            var metrics = circuitBreaker.getMetrics();
            return String.format("State: %s, Failure Rate: %.1f%%, Buffered Calls: %d",
                    circuitBreaker.getState(),
                    metrics.getFailureRate() * 100,
                    metrics.getNumberOfBufferedCalls());
        } catch (Exception e) {
            log.error("Failed to get circuit breaker metrics for {}: {}", circuitBreakerName, e.getMessage());
            return "Metrics unavailable";
        }
    }

    private boolean shouldRetryLater(Exception ex) {
        return !(ex instanceof IllegalArgumentException ||
                ex instanceof UnsupportedOperationException);
    }

    // ====== MONITORING ======

    @EventListener
    public void onCircuitBreakerEvent(CircuitBreakerEvent event) {
        log.info("Circuit Breaker Event: {} - {} - {}",
                event.getCircuitBreakerName(),
                event.getEventType(),
                event.getCreationTime());
    }

    @EventListener
    public void onCircuitBreakerStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.warn("Circuit Breaker State Transition: {} - From: {} → To: {} - {}",
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState(),
                event.getCreationTime());
    }

    @Override
    public void confirmInventoryReservation(String orderNumber) {
        log.info("Confirming inventory reservation for order: {}", orderNumber);
        inventoryClient.confirmReservation(orderNumber);
    }

    private ShippingResponse shippingResponseMapper(DeliveryResponse response) {
        Boolean success = response != null && (response.status().equals("PENDING")
                || response.status().equals("SHIPPED") || response.status().equals("DELIVERED"));
        return new ShippingResponse(success, response.trackingNumber(), "Shipping arranged successfully", true,
                response.createdAt());
    }
}
