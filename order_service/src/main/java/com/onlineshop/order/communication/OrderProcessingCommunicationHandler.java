package com.onlineshop.order.communication;

import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.InventoryResponse;
import com.onlineshop.order.dto.response.PaymentResponse;
import com.onlineshop.order.dto.response.ShippingResponse;

/**
 * Strategy interface for communication with external services.
 * Allows switching between REST, Event-driven, or other communication patterns.
 */
public interface OrderProcessingCommunicationHandler {

    /**
     * Reserve inventory for the order
     * 
     * @param request Inventory reservation request
     * @return Inventory response with transaction ID
     */
    InventoryResponse reserveInventory(InventoryRequest request);

    /**
     * Release reserved inventory (compensation)
     * 
     * @param transactionId Transaction ID from reservation
     * @return Success status
     */
    InventoryResponse releaseInventory(String transactionId);

    /**
     * Process payment for the order
     * 
     * @param request Payment processing request
     * @return Payment response with transaction ID
     */
    PaymentResponse processPayment(PaymentRequest request);

    /**
     * Refund payment (compensation)
     * 
     * @param transactionId Transaction ID from payment
     * @return Success status
     */
    PaymentResponse refundPayment(String transactionId);

    /**
     * Arrange shipping for the order
     * 
     * @param request Shipping arrangement request
     * @return Shipping response with tracking number
     */
    ShippingResponse arrangeShipping(ShippingRequest request);

    /**
     * Cancel shipping (compensation)
     * 
     * @param trackingNumber Tracking number from shipping arrangement
     * @return Success status
     */
    ShippingResponse cancelShipping(String trackingNumber);

    void confirmInventoryReservation(String orderNumber);
}
