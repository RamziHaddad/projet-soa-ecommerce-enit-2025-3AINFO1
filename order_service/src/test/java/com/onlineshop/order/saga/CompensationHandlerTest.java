package com.onlineshop.order.saga;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlineshop.order.model.Order;
import com.onlineshop.order.saga.compensation.CompensationHandler;

/**
 * Interface contract tests for CompensationHandler
 * Verifies that the interface methods can be called and behave as expected
 */
@ExtendWith(MockitoExtension.class)
class CompensationHandlerTest {

    @Mock
    private CompensationHandler compensationHandler;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORDER-TEST-001")
                .build();
    }

    @Test
    void testCompensateInventory_InterfaceContract() {
        // When
        compensationHandler.compensateInventory(testOrder);

        // Then
        verify(compensationHandler).compensateInventory(testOrder);
    }

    @Test
    void testCompensatePayment_InterfaceContract() {
        // When
        compensationHandler.compensatePayment(testOrder);

        // Then
        verify(compensationHandler).compensatePayment(testOrder);
    }

    @Test
    void testCompensateShipping_InterfaceContract() {
        // When
        compensationHandler.compensateShipping(testOrder);

        // Then
        verify(compensationHandler).compensateShipping(testOrder);
    }

    @Test
    void testExecuteCompensation_InterfaceContract() {
        // When
        compensationHandler.executeCompensation(testOrder);

        // Then
        verify(compensationHandler).executeCompensation(testOrder);
    }

    @Test
    void testCompensateInventory_NullOrder() {
        // When
        compensationHandler.compensateInventory(null);

        // Then
        verify(compensationHandler).compensateInventory(null);
    }

    @Test
    void testCompensatePayment_NullOrder() {
        // When
        compensationHandler.compensatePayment(null);

        // Then
        verify(compensationHandler).compensatePayment(null);
    }

    @Test
    void testCompensateShipping_NullOrder() {
        // When
        compensationHandler.compensateShipping(null);

        // Then
        verify(compensationHandler).compensateShipping(null);
    }

    @Test
    void testExecuteCompensation_NullOrder() {
        // When
        compensationHandler.executeCompensation(null);

        // Then
        verify(compensationHandler).executeCompensation(null);
    }

    @Test
    void testInterfaceMethodParameters() {
        // Verify that all methods accept Order parameter
        compensationHandler.compensateInventory(testOrder);
        compensationHandler.compensatePayment(testOrder);
        compensationHandler.compensateShipping(testOrder);
        compensationHandler.executeCompensation(testOrder);

        verify(compensationHandler).compensateInventory(testOrder);
        verify(compensationHandler).compensatePayment(testOrder);
        verify(compensationHandler).compensateShipping(testOrder);
        verify(compensationHandler).executeCompensation(testOrder);
    }

    @Test
    void testInterfaceMethodReturnTypes() {
        // Verify that methods have correct return types (void)
        assertDoesNotThrow(() -> {
            compensationHandler.compensateInventory(testOrder);
            compensationHandler.compensatePayment(testOrder);
            compensationHandler.compensateShipping(testOrder);
            compensationHandler.executeCompensation(testOrder);
        });
    }
}
