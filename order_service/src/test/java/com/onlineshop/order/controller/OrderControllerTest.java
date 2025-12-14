package com.onlineshop.order.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlineshop.order.controller.OrderController;
import com.onlineshop.order.dto.request.OrderItemRequest;
import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.dto.response.OrderItemResponse;
import com.onlineshop.order.dto.response.OrderResponse;
import com.onlineshop.order.exception.OrderNotFoundException;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderRequest orderRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .build();
        
        orderRequest = OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Main St, City, State 12345")
                .items(Arrays.asList(itemRequest))
                .build();
        
        OrderItemResponse itemResponse = OrderItemResponse.builder()
                .id(1L)
                .productId(1L)
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .subtotal(new BigDecimal("59.98"))
                .build();
        
        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-2025-001")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.98"))
                .shippingAddress("123 Main St, City, State 12345")
                .items(Arrays.asList(itemResponse))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateOrder() throws Exception {
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderResponse.getId()))
                .andExpect(jsonPath("$.orderNumber").value(orderResponse.getOrderNumber()))
                .andExpect(jsonPath("$.customerId").value(orderResponse.getCustomerId()))
                .andExpect(jsonPath("$.status").value(orderResponse.getStatus().toString()));

        verify(orderService).createOrder(any(OrderRequest.class));
    }

    @Test
    void testGetOrderById() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderResponse.getId()))
                .andExpect(jsonPath("$.orderNumber").value(orderResponse.getOrderNumber()));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void testGetOrderByNumber() throws Exception {
        when(orderService.getOrderByNumber("ORD-2025-001")).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/number/ORD-2025-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderResponse.getOrderNumber()));

        verify(orderService).getOrderByNumber("ORD-2025-001");
    }

    @Test
    void testGetOrdersByCustomerId() throws Exception {
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrdersByCustomerId(1L)).thenReturn(orders);

        mockMvc.perform(get("/api/orders/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(1L));

        verify(orderService).getOrdersByCustomerId(1L);
    }

    @Test
    void testCancelOrder() throws Exception {
        doNothing().when(orderService).cancelOrder(1L);

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrder(1L);
    }

    @Test
    void testInvalidOrderRequest() throws Exception {
        OrderRequest invalidRequest = OrderRequest.builder()
                .customerId(null) // Invalid - null customerId
                .shippingAddress("") // Invalid - empty shipping address
                .items(Arrays.asList()) // Invalid - empty items list
                .build();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOrderNotFound() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new OrderNotFoundException("Order not found with id: 999"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(999L);
    }
}
