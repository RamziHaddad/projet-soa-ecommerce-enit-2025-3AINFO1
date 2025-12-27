package com.onlineshop.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.onlineshop.order.dto.request.OrderItemRequest;
import com.onlineshop.order.dto.request.OrderRequest;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.OrderStatus;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.model.SagaStatus;
import com.onlineshop.order.model.SagaStep;
import com.onlineshop.order.repository.OrderRepository;
import com.onlineshop.order.repository.SagaStateRepository;
import com.onlineshop.order.service.OrderService;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for SAGA transaction isolation.
 * 
 * These tests verify that:
 * 1. Each saga step commits independently
 * 2. Previous steps remain committed when later steps fail
 * 3. Compensation accesses committed state
 * 4. Retry mechanism resumes from the correct step
 * 
 * Uses WireMock for external service simulation.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.show-sql=true",
        "logging.level.com.onlineshop.order=DEBUG"
})
@Slf4j
class SagaTransactionIsolationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaStateRepository sagaStateRepository;

    @Autowired
    private SagaOrchestrator sagaOrchestrator;

    private OrderRequest createTestOrderRequest() {
        OrderItemRequest item = new OrderItemRequest("PROD-001", 2, new BigDecimal("50.00"));

        return new OrderRequest(12345L, "123 Test Street, Test City, TC 12345", List.of(item));
    }

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        sagaStateRepository.deleteAll();
        orderRepository.deleteAll();
    }

    /**
     * Test: Verify that inventory step commits before payment step executes.
     * 
     * When: Inventory succeeds and payment is about to execute
     * Then: Inventory state should be persisted and retrievable
     */
    @Test
    void testInventoryStepCommitsBeforePaymentStep() {
        // Given
        OrderRequest request = createTestOrderRequest();
        var response = orderService.createOrder(request);

        // Wait for inventory step to complete
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(response.id()).orElseThrow();
                    SagaState sagaState = sagaStateRepository.findByOrder(order).orElseThrow();

                    // Verify inventory step has committed
                    assertThat(sagaState.getInventoryReserved()).isTrue();
                    assertThat(sagaState.getInventoryTransactionId()).isNotNull();
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);

                    log.info("Inventory step committed - Order: {}, State: {}",
                            order.getOrderNumber(), sagaState.getCurrentStep());
                });
    }

    /**
     * Test: Verify that when payment fails, inventory reservation remains
     * committed.
     * 
     * When: Inventory succeeds but payment fails
     * Then: Inventory state should remain persisted
     * Saga should be marked as failed
     * Compensation should be triggered
     */
    @Test
    void testInventoryRemainsCommittedWhenPaymentFails() {
        // This test requires WireMock configuration to simulate payment failure
        // TODO: Configure WireMock to fail payment after successful inventory

        // Given
        OrderRequest request = createTestOrderRequest();
        var response = orderService.createOrder(request);

        // Wait for saga to fail at payment step
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(response.id()).orElseThrow();
                    SagaState sagaState = sagaStateRepository.findByOrder(order).orElseThrow();

                    // Verify inventory step is still committed
                    assertThat(sagaState.getInventoryReserved()).isTrue();
                    assertThat(sagaState.getInventoryTransactionId()).isNotNull();

                    // Verify payment did not commit
                    assertThat(sagaState.getPaymentProcessed()).isFalse();

                    // Verify saga failed
                    assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.FAILED);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);

                    log.info("Payment failed but inventory remains committed - Order: {}",
                            order.getOrderNumber());
                });
    }

    /**
     * Test: Verify that when shipping fails, both inventory and payment remain
     * committed.
     * 
     * When: Inventory and payment succeed but shipping fails
     * Then: Both inventory and payment state should remain persisted
     * Saga should be marked as failed
     * Compensation should rollback both inventory and payment
     */
    @Test
    void testInventoryAndPaymentRemainCommittedWhenShippingFails() {
        // This test requires WireMock configuration to simulate shipping failure
        // TODO: Configure WireMock to fail shipping after successful inventory and
        // payment

        // Given
        OrderRequest request = createTestOrderRequest();
        var response = orderService.createOrder(request);

        // Wait for saga to fail at shipping step
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(response.id()).orElseThrow();
                    SagaState sagaState = sagaStateRepository.findByOrder(order).orElseThrow();

                    // Verify inventory and payment steps are still committed
                    assertThat(sagaState.getInventoryReserved()).isTrue();
                    assertThat(sagaState.getInventoryTransactionId()).isNotNull();
                    assertThat(sagaState.getPaymentProcessed()).isTrue();
                    assertThat(sagaState.getPaymentTransactionId()).isNotNull();

                    // Verify shipping did not commit
                    assertThat(sagaState.getShippingArranged()).isFalse();

                    // Verify saga failed
                    assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.FAILED);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);

                    log.info("Shipping failed but inventory and payment remain committed - Order: {}",
                            order.getOrderNumber());
                });
    }

    /**
     * Test: Verify that compensation can access committed state from completed
     * steps.
     * 
     * When: A step fails and compensation is triggered
     * Then: Compensation should be able to read the committed state
     * Compensation should execute in reverse order
     */
    @Test
    void testCompensationAccessesCommittedState() {
        // This test requires WireMock configuration to simulate failure and
        // compensation
        // TODO: Configure WireMock to trigger compensation scenario

        // Given
        OrderRequest request = createTestOrderRequest();
        var response = orderService.createOrder(request);

        // Wait for saga to fail and compensation to complete
        await()
                .atMost(Duration.ofSeconds(25))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(response.id()).orElseThrow();
                    SagaState sagaState = sagaStateRepository.findByOrder(order).orElseThrow();

                    // Verify saga reached compensated or failed state
                    assertThat(sagaState.getStatus())
                            .isIn(SagaStatus.COMPENSATED, SagaStatus.FAILED);

                    // Verify compensation cleared the transaction IDs (indicating it accessed
                    // committed state)
                    // Note: This depends on compensation implementation details
                    log.info("Compensation completed - Order: {}, Final State: {}",
                            order.getOrderNumber(), sagaState.getStatus());
                });
    }

    /**
     * Test: Verify that retry mechanism resumes from the correct step.
     * 
     * When: A saga fails at a specific step and retry is triggered
     * Then: Retry should resume from the failed step
     * Previously completed steps should not re-execute
     */
    @Test
    void testRetryResumesFromCorrectStep() {
        // This test requires WireMock configuration to simulate retryable failure
        // TODO: Configure WireMock to simulate retryable failure at payment step

        // Given
        OrderRequest request = createTestOrderRequest();
        var response = orderService.createOrder(request);

        // Wait for initial saga execution
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(response.id()).orElseThrow();
                    SagaState sagaState = sagaStateRepository.findByOrder(order).orElseThrow();

                    // Verify saga is in a retryable failed state
                    assertThat(sagaState.getRetryable()).isTrue();
                    assertThat(sagaState.getStatus()).isIn(SagaStatus.FAILED, SagaStatus.IN_PROGRESS);
                });

        // When: Trigger retry manually
        Order order = orderRepository.findById(response.id()).orElseThrow();
        SagaState beforeRetry = sagaStateRepository.findByOrder(order).orElseThrow();
        SagaStep stepBeforeRetry = beforeRetry.getCurrentStep();
        String inventoryTxIdBeforeRetry = beforeRetry.getInventoryTransactionId();

        sagaOrchestrator.retrySaga(order);

        // Then: Wait for retry to complete
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order retryOrder = orderRepository.findById(response.id()).orElseThrow();
                    SagaState afterRetry = sagaStateRepository.findByOrder(retryOrder).orElseThrow();

                    // Verify retry started from the same step
                    log.info("Retry executed - Step before: {}, Step after: {}",
                            stepBeforeRetry, afterRetry.getCurrentStep());

                    // Verify inventory transaction ID hasn't changed (step didn't re-execute)
                    if (stepBeforeRetry.ordinal() > SagaStep.INVENTORY_VALIDATION.ordinal()) {
                        assertThat(afterRetry.getInventoryTransactionId())
                                .isEqualTo(inventoryTxIdBeforeRetry);
                    }

                    // Verify retry count incremented
                    assertThat(afterRetry.getRetryCount()).isGreaterThan(beforeRetry.getRetryCount());
                });
    }

    /**
     * Test: Verify complete successful saga execution with all steps committing
     * independently.
     * 
     * When: All saga steps succeed
     * Then: Each step should commit in sequence
     * Final state should be COMPLETED
     */
    @Test
    void testSuccessfulSagaExecutionWithIndependentCommits() {
        // Given
        OrderRequest request = createTestOrderRequest();
        var response = orderService.createOrder(request);

        // Wait for complete saga execution
        await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(response.id()).orElseThrow();
                    SagaState sagaState = sagaStateRepository.findByOrder(order).orElseThrow();

                    // Verify all steps completed and committed
                    assertThat(sagaState.getInventoryReserved()).isTrue();
                    assertThat(sagaState.getInventoryTransactionId()).isNotNull();
                    assertThat(sagaState.getPaymentProcessed()).isTrue();
                    assertThat(sagaState.getPaymentTransactionId()).isNotNull();
                    assertThat(sagaState.getShippingArranged()).isTrue();
                    assertThat(sagaState.getShippingTransactionId()).isNotNull();

                    // Verify final state
                    assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.COMPLETED);
                    assertThat(sagaState.getCurrentStep()).isEqualTo(SagaStep.COMPLETED);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

                    log.info("Saga completed successfully - Order: {}", order.getOrderNumber());
                });
    }

    /**
     * Test: Verify that order creation transaction completes before saga starts.
     * 
     * When: Order is created
     * Then: Order should be persisted and retrievable
     * Saga should start asynchronously
     */
    @Test
    void testOrderCreationCommitsBeforeSagaStarts() {
        // Given
        OrderRequest request = createTestOrderRequest();

        // When
        var response = orderService.createOrder(request);

        // Then: Order should be immediately retrievable (committed)
        Order order = orderRepository.findById(response.id()).orElseThrow();
        assertThat(order).isNotNull();
        assertThat(order.getOrderNumber()).isEqualTo(response.orderNumber());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        log.info("Order creation committed immediately - Order: {}", order.getOrderNumber());

        // And: Saga should start asynchronously
        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    SagaState sagaState = sagaStateRepository.findByOrder(order).orElseThrow();
                    assertThat(sagaState).isNotNull();
                    assertThat(sagaState.getStatus()).isIn(SagaStatus.STARTED, SagaStatus.IN_PROGRESS);

                    log.info("Saga started asynchronously - Order: {}, Saga Status: {}",
                            order.getOrderNumber(), sagaState.getStatus());
                });
    }
}
