package com.onlineshop.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotNull(message = "Customer ID is required") Long customerId,

        @NotBlank(message = "Shipping address is required") String shippingAddress,

        @NotEmpty(message = "Order must contain at least one item") @Valid List<OrderItemRequest> items) {
}
