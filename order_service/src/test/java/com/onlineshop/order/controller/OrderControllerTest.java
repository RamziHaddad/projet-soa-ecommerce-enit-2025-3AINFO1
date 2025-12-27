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
                OrderItemRequest itemRequest = new OrderItemRequest("PROD-001", 2, new BigDecimal("29.99"));

                orderRequest = new OrderRequest(1L, "123 Main St, City, State 12345", Arrays.asList(itemRequest));

                OrderItemResponse itemResponse = new OrderItemResponse(1L, "PROD-001", "Test Product", 2,
                                new BigDecimal("29.99"), new BigDecimal("59.98"));

                orderResponse = new OrderResponse(1L, "ORD-2025-001", 1L, OrderStatus.PENDING, new BigDecimal("59.98"),
                                "123 Main St, City, State 12345", Arrays.asList(itemResponse), LocalDateTime.now(),
                                LocalDateTime.now());
        }

        @Test
        void testCreateOrder() throws Exception {
                when(orderService.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);

                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(orderResponse.id()))
                                .andExpect(jsonPath("$.orderNumber").value(orderResponse.orderNumber()))
                                .andExpect(jsonPath("$.customerId").value(orderResponse.customerId()))
                                .andExpect(jsonPath("$.status").value(orderResponse.status().toString()));

                verify(orderService).createOrder(any(OrderRequest.class));
        }

        @Test
        void testGetOrderById() throws Exception {
                when(orderService.getOrderById(1L)).thenReturn(orderResponse);

                mockMvc.perform(get("/api/orders/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(orderResponse.id()))
                                .andExpect(jsonPath("$.orderNumber").value(orderResponse.orderNumber()));

                verify(orderService).getOrderById(1L);
        }

        @Test
        void testGetOrderByNumber() throws Exception {
                when(orderService.getOrderByNumber("ORD-2025-001")).thenReturn(orderResponse);

                mockMvc.perform(get("/api/orders/number/ORD-2025-001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderNumber").value(orderResponse.orderNumber()));

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
                OrderRequest invalidRequest = new OrderRequest(null, "", Arrays.asList());

                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testOrderNotFound() throws Exception {
                when(orderService.getOrderById(999L))
                                .thenThrow(new OrderNotFoundException("Order not found with id: 999"));

                mockMvc.perform(get("/api/orders/999"))
                                .andExpect(status().isNotFound());

                verify(orderService).getOrderById(999L);
        }
}
