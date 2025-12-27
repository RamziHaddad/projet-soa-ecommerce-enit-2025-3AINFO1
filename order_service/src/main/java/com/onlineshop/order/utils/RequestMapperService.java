package com.onlineshop.order.utils;

import org.springframework.stereotype.Component;

import com.onlineshop.order.dto.request.InventoryItemRequest;
import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.model.Order;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for mapping Order entities to external service request
 * DTOs.
 * Provides a clean separation between domain models and external service
 * contracts.
 */
@Component
@Slf4j
public class RequestMapperService {

        /**
         * Maps an Order to an InventoryRequest.
         *
         * @param order The order to map
         * @return The inventory request DTO
         */
        public InventoryRequest mapToInventoryRequest(Order order) {
                log.debug("Mapping order {} to inventory request", order.getOrderNumber());

                return new InventoryRequest(
                                order.getOrderNumber(),
                                order.getItems().stream()
                                                .map(item -> new InventoryItemRequest(
                                                                item.getProductId(),
                                                                item.getQuantity()))
                                                .toList());
        }

        /**
         * Maps an Order to a PaymentRequest.
         *
         * @param order The order to map
         * @return The payment request DTO
         */
        public PaymentRequest mapToPaymentRequest(Order order) {
                log.debug("Mapping order {} to payment request", order.getOrderNumber());

                return new PaymentRequest(
                                order.getOrderNumber(),
                                order.getCustomerId(),
                                order.getTotalAmount(),
                                null);
        }

        /**
         * Maps an Order to a ShippingRequest.
         *
         * @param order The order to map
         * @return The shipping request DTO
         */
        public ShippingRequest mapToShippingRequest(Order order) {
                log.debug("Mapping order {} to shipping request", order.getOrderNumber());

                return new ShippingRequest(
                                order.getOrderNumber(),
                                order.getCustomerId(),
                                order.getShippingAddress());
        }
}
