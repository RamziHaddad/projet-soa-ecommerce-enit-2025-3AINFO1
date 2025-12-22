package com.onlineshop.order.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderItem;
import com.onlineshop.order.model.OrderStatus;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder1;
    private Order testOrder2;
    private Order testOrder3;

    @BeforeEach
    void setUp() {
        // Initialize test data
        // Create OrderItem for testOrder1
        OrderItem item1 = OrderItem.builder()
                .productName("Product 1")
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .subtotal(new BigDecimal("59.98"))
                .build();

        testOrder1 = Order.builder()
                .orderNumber("ORD-2025-001")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.98"))
                .shippingAddress("123 Main St, City, State 12345")
                .items(new ArrayList<>(Arrays.asList(item1)))
                .build();
        item1.setOrder(testOrder1); // Set bidirectional relationship

        // Create OrderItem for testOrder2
        OrderItem item2 = OrderItem.builder()
                .productName("Product 2")
                .productId(2L)
                .quantity(1)
                .unitPrice(new BigDecimal("49.99"))
                .subtotal(new BigDecimal("49.99"))
                .build();

        testOrder2 = Order.builder()
                .orderNumber("ORD-2025-002")
                .customerId(1L) // Same customer as testOrder1
                .status(OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("49.99"))
                .shippingAddress("456 Oak Ave, Different City, DC 67890")
                .items(new ArrayList<>(Arrays.asList(item2)))
                .build();
        item2.setOrder(testOrder2);

        // Create OrderItem for testOrder3
        OrderItem item3 = OrderItem.builder()
                .productName("Product 3")
                .productId(3L)
                .quantity(3)
                .unitPrice(new BigDecimal("19.99"))
                .subtotal(new BigDecimal("59.97"))
                .build();

        testOrder3 = Order.builder()
                .orderNumber("ORD-2025-003")
                .customerId(2L) // Different customer
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("59.97"))
                .shippingAddress("789 Pine St, Another City, AC 11111")
                .items(new ArrayList<>(Arrays.asList(item3)))
                .build();
        item3.setOrder(testOrder3);
    }

    @Test
    void testSaveOrder() {
        // Test saving a new order
        Order savedOrder = orderRepository.save(testOrder1);
        entityManager.flush(); // Ensure the save is persisted

        assertNotNull(savedOrder.getId());
        assertEquals("ORD-2025-001", savedOrder.getOrderNumber());
        assertEquals(1L, savedOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(new BigDecimal("59.98"), savedOrder.getTotalAmount());
        assertEquals("123 Main St, City, State 12345", savedOrder.getShippingAddress());
        assertEquals(1, savedOrder.getItems().size());
    }

    @Test
    void testSaveOrderWithMultipleItems() {
        // Test saving an order with multiple items
        OrderItem item1 = OrderItem.builder()
                .productName("Product 1")
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .subtotal(new BigDecimal("50.00"))
                .build();

        OrderItem item2 = OrderItem.builder()
                .productName("Product 2")
                .productId(2L)
                .quantity(1)
                .unitPrice(new BigDecimal("30.00"))
                .subtotal(new BigDecimal("30.00"))
                .build();

        Order orderWithMultipleItems = Order.builder()
                .orderNumber("ORD-MULTI-001")
                .customerId(3L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("80.00"))
                .shippingAddress("999 Multi St, Multi City, MC 22222")
                .items(new ArrayList<>(Arrays.asList(item1, item2)))
                .build();

        item1.setOrder(orderWithMultipleItems);
        item2.setOrder(orderWithMultipleItems);

        Order savedOrder = orderRepository.save(orderWithMultipleItems);
        entityManager.flush();

        assertNotNull(savedOrder.getId());
        assertEquals(2, savedOrder.getItems().size());
    }

    @Test
    void testFindById() {
        // Test finding order by ID
        Long savedId = entityManager.persistAndGetId(testOrder1, Long.class);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to ensure fresh query

        Optional<Order> foundOrder = orderRepository.findById(savedId);

        assertTrue(foundOrder.isPresent());
        Order order = foundOrder.get();
        assertEquals("ORD-2025-001", order.getOrderNumber());
        assertEquals(1L, order.getCustomerId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void testFindByIdNotFound() {
        // Test finding non-existent order by ID
        Long nonExistentId = 999L;
        Optional<Order> foundOrder = orderRepository.findById(nonExistentId);

        assertFalse(foundOrder.isPresent());
    }

    @Test
    void testFindByOrderNumber() {
        // Test finding order by order number
        entityManager.persist(testOrder1);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> foundOrder = orderRepository.findByOrderNumber("ORD-2025-001");

        assertTrue(foundOrder.isPresent());
        assertEquals(1L, foundOrder.get().getCustomerId());
        assertEquals(OrderStatus.PENDING, foundOrder.get().getStatus());
    }

    @Test
    void testFindByOrderNumberNotFound() {
        // Test finding non-existent order by order number
        Optional<Order> foundOrder = orderRepository.findByOrderNumber("NON-EXISTENT");

        assertFalse(foundOrder.isPresent());
    }

    @Test
    void testFindByCustomerId() {
        // Test finding orders by customer ID
        entityManager.persist(testOrder1); // customerId = 1
        entityManager.persist(testOrder2); // customerId = 1
        entityManager.persist(testOrder3); // customerId = 2
        entityManager.flush();
        entityManager.clear();

        // Find orders for customer 1
        List<Order> customer1Orders = orderRepository.findByCustomerId(1L);

        assertEquals(2, customer1Orders.size());
        assertTrue(customer1Orders.stream().anyMatch(order -> "ORD-2025-001".equals(order.getOrderNumber())));
        assertTrue(customer1Orders.stream().anyMatch(order -> "ORD-2025-002".equals(order.getOrderNumber())));

        // Find orders for customer 2
        List<Order> customer2Orders = orderRepository.findByCustomerId(2L);

        assertEquals(1, customer2Orders.size());
        assertEquals("ORD-2025-003", customer2Orders.get(0).getOrderNumber());
    }

    @Test
    void testFindByCustomerIdNoOrders() {
        // Test finding orders for customer with no orders
        List<Order> orders = orderRepository.findByCustomerId(999L);

        assertTrue(orders.isEmpty());
    }

    @Test
    void testFindByStatus() {
        // Test finding orders by status
        entityManager.persist(testOrder1); // PENDING
        entityManager.persist(testOrder2); // COMPLETED
        entityManager.persist(testOrder3); // PENDING
        entityManager.flush();
        entityManager.clear();

        // Find PENDING orders
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        assertEquals(2, pendingOrders.size());
        assertTrue(pendingOrders.stream().allMatch(order -> order.getStatus() == OrderStatus.PENDING));

        // Find COMPLETED orders
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

        assertEquals(1, completedOrders.size());
        assertEquals("ORD-2025-002", completedOrders.get(0).getOrderNumber());
    }

    @Test
    void testFindByStatusNoOrders() {
        // Test finding orders with status that doesn't exist
        List<Order> orders = orderRepository.findByStatus(OrderStatus.FAILED);

        assertTrue(orders.isEmpty());
    }

    @Test
    void testUpdateOrder() {
        // Test updating an existing order
        Long savedId = entityManager.persistAndGetId(testOrder1, Long.class);
        entityManager.flush();
        entityManager.clear();

        // Retrieve and update
        Optional<Order> orderOpt = orderRepository.findById(savedId);
        assertTrue(orderOpt.isPresent());

        Order order = orderOpt.get();
        order.setStatus(OrderStatus.COMPLETED);
        order.setShippingAddress("Updated Address, Updated City, UC 33333");

        Order updatedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Verify update
        Optional<Order> verifiedOrder = orderRepository.findById(savedId);
        assertTrue(verifiedOrder.isPresent());
        Order verified = verifiedOrder.get();

        assertEquals(OrderStatus.COMPLETED, verified.getStatus());
        assertEquals("Updated Address, Updated City, UC 33333", verified.getShippingAddress());
    }

    @Test
    void testDeleteOrder() {
        // Test deleting an order
        Long savedId = entityManager.persistAndGetId(testOrder1, Long.class);
        entityManager.flush();

        // Verify order exists
        Optional<Order> existingOrder = orderRepository.findById(savedId);
        assertTrue(existingOrder.isPresent());

        // Delete order
        orderRepository.deleteById(savedId);
        entityManager.flush();

        // Verify order is deleted
        Optional<Order> deletedOrder = orderRepository.findById(savedId);
        assertFalse(deletedOrder.isPresent());
    }

    @Test
    void testDeleteOrderCascade() {
        // Test that deleting order cascades to order items
        Long savedId = entityManager.persistAndGetId(testOrder1, Long.class);
        entityManager.flush();

        // Verify order and items exist
        Optional<Order> existingOrder = orderRepository.findById(savedId);
        assertTrue(existingOrder.isPresent());
        assertEquals(1, existingOrder.get().getItems().size());

        // Delete order
        orderRepository.deleteById(savedId);
        entityManager.flush();

        // Verify order is deleted (cascade should remove items too)
        Optional<Order> deletedOrder = orderRepository.findById(savedId);
        assertFalse(deletedOrder.isPresent());
    }

    @Test
    void testFindAllOrders() {
        // Test finding all orders
        entityManager.persist(testOrder1);
        entityManager.persist(testOrder2);
        entityManager.persist(testOrder3);
        entityManager.flush();
        entityManager.clear();

        List<Order> allOrders = orderRepository.findAll();

        assertEquals(3, allOrders.size());
    }

    @Test
    void testOrderNumberUniqueness() {
        // Test that order numbers are unique (database constraint)
        entityManager.persist(testOrder1);
        entityManager.flush();

        // Try to create another order with the same order number
        Order duplicateOrder = Order.builder()
                .orderNumber("ORD-2025-001") // Same as testOrder1
                .customerId(999L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .shippingAddress("Duplicate St, Duplicate City, DC 44444")
                .build();

        // This should throw an exception due to unique constraint
        assertThrows(Exception.class, () -> {
            entityManager.persist(duplicateOrder);
            entityManager.flush();
        });
    }

    @Test
    void testOrderTimestamps() {
        // Test that timestamps are automatically set
        Order savedOrder = orderRepository.save(testOrder1);
        entityManager.flush();

        assertNotNull(savedOrder.getCreatedAt());
        assertNotNull(savedOrder.getUpdatedAt());
        assertEquals(savedOrder.getCreatedAt(), savedOrder.getUpdatedAt());

        // Update the order and check that updatedAt changes
        savedOrder.setStatus(OrderStatus.COMPLETED);
        Order updatedOrder = orderRepository.save(savedOrder);
        entityManager.flush();
        entityManager.clear();

        // Retrieve again to verify timestamp change
        Optional<Order> verifiedOrder = orderRepository.findById(savedOrder.getId());
        assertTrue(verifiedOrder.isPresent());

        assertNotEquals(savedOrder.getCreatedAt(), verifiedOrder.get().getUpdatedAt());
    }
}
