package com.example.cart.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderRequest {
    public Long customerId;
    public String shippingAddress;
    public List<OrderItem> items = new ArrayList<>();

    public static class OrderItem {
        public Long productId;
        public int quantity;
        public BigDecimal unitPrice;
    }
}
