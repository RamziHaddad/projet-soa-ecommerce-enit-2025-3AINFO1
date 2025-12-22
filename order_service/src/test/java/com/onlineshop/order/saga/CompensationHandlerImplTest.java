package com.onlineshop.order.saga;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlineshop.order.communication.OrderProcessingCommunicationHandler;
import com.onlineshop.order.dto.response.InventoryResponse;
import com.onlineshop.order.dto.response.PaymentResponse;
import com.onlineshop.order.dto.response.ShippingResponse;
import com.onlineshop.order.model.Order;
import com.onlineshop.order.model.SagaState;
import com.onlineshop.order.repository.SagaStateRepository;
import com.onlineshop.order.saga.compensation.CompensationHandlerImpl;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CompensationHandlerImplTest {

    @Mock
    private OrderProcessingCommunicationHandler communicationStrategy;

    @Mock
    private SagaStateRepository sagaStateRepository;

    @InjectMocks
    private CompensationHandlerImpl compensationHandler;

    @Captor
    private ArgumentCaptor<SagaState> sagaStateCaptor;

    private Order testOrder;
    private SagaState testSagaState;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORDER-TEST-001")
                .customerId(123L)
                .totalAmount(BigDecimal.valueOf(100.00))
                .shippingAddress("Test Address")
                .build();

        testSagaState = SagaState.builder()
                .id(1L)
                .order(testOrder)
                .inventoryReserved(false)
                .paymentProcessed(false)
                .shippingArranged(false)
                .inventoryTransactionId(null)
                .paymentTransactionId(null)
                .shippingTransactionId(null)
                .build();
    }

    @Test
    void testCompensateInventory_Success() {
        // Given
        testSagaState.setInventoryReserved(true);
        testSagaState.setInventoryTransactionId("INV-TRANS-001");

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        InventoryResponse releaseResponse = InventoryResponse.builder()
                .success(true)
                .message("Inventory released successfully")
                .build();
        when(communicationStrategy.releaseInventory("INV-TRANS-001")).thenReturn(releaseResponse);

        // When
        compensationHandler.compensateInventory(testOrder);

        // Then
        verify(communicationStrategy).releaseInventory("INV-TRANS-001");
        verify(sagaStateRepository).save(sagaStateCaptor.capture());

        SagaState savedState = sagaStateCaptor.getValue();
        assertFalse(savedState.getInventoryReserved());
        assertNull(savedState.getInventoryTransactionId());
    }

    @Test
    void testCompensateInventory_Failure() {
        // Given
        testSagaState.setInventoryReserved(true);
        testSagaState.setInventoryTransactionId("INV-TRANS-001");

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        InventoryResponse releaseResponse = InventoryResponse.builder()
                .success(false)
                .message("Release failed")
                .build();
        when(communicationStrategy.releaseInventory("INV-TRANS-001")).thenReturn(releaseResponse);

        // When
        compensationHandler.compensateInventory(testOrder);

        // Then
        verify(communicationStrategy).releaseInventory("INV-TRANS-001");
        verify(sagaStateRepository, never()).save(any()); // Should not save if release fails
    }

    @Test
    void testCompensateInventory_NotReserved() {
        // Given
        testSagaState.setInventoryReserved(false);
        testSagaState.setInventoryTransactionId(null);

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        // When
        compensationHandler.compensateInventory(testOrder);

        // Then
        verify(communicationStrategy, never()).releaseInventory(anyString());
        verify(sagaStateRepository, never()).save(any());
    }

    @Test
    void testCompensatePayment_Success() {
        // Given
        testSagaState.setPaymentProcessed(true);
        testSagaState.setPaymentTransactionId("PAY-TRANS-001");

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        PaymentResponse refundResponse = PaymentResponse.builder()
                .success(true)
                .message("Payment refunded successfully")
                .build();
        when(communicationStrategy.refundPayment("PAY-TRANS-001")).thenReturn(refundResponse);

        // When
        compensationHandler.compensatePayment(testOrder);

        // Then
        verify(communicationStrategy).refundPayment("PAY-TRANS-001");
        verify(sagaStateRepository).save(sagaStateCaptor.capture());

        SagaState savedState = sagaStateCaptor.getValue();
        assertFalse(savedState.getPaymentProcessed());
        assertNull(savedState.getPaymentTransactionId());
    }

    @Test
    void testCompensatePayment_Failure() {
        // Given
        testSagaState.setPaymentProcessed(true);
        testSagaState.setPaymentTransactionId("PAY-TRANS-001");

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        PaymentResponse refundResponse = PaymentResponse.builder()
                .success(false)
                .message("Refund failed")
                .build();
        when(communicationStrategy.refundPayment("PAY-TRANS-001")).thenReturn(refundResponse);

        // When
        compensationHandler.compensatePayment(testOrder);

        // Then
        verify(communicationStrategy).refundPayment("PAY-TRANS-001");
        verify(sagaStateRepository, never()).save(any());
    }

    @Test
    void testCompensateShipping_Success() {
        // Given
        testSagaState.setShippingArranged(true);
        testSagaState.setShippingTransactionId("SHIPPING-001");

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        ShippingResponse cancelResponse = ShippingResponse.builder()
                .success(true)
                .message("Shipping cancelled successfully")
                .build();
        when(communicationStrategy.cancelShipping("SHIPPING-001")).thenReturn(cancelResponse);

        // When
        compensationHandler.compensateShipping(testOrder);

        // Then
        verify(communicationStrategy).cancelShipping("SHIPPING-001");
        verify(sagaStateRepository).save(sagaStateCaptor.capture());

        SagaState savedState = sagaStateCaptor.getValue();
        assertFalse(savedState.getShippingArranged());
        assertNull(savedState.getShippingTransactionId());
    }

    @Test
    void testExecuteCompensation_FullWorkflow() {
        // Given
        testSagaState.setShippingArranged(true);
        testSagaState.setShippingTransactionId("SHIPPING-001");
        testSagaState.setPaymentProcessed(true);
        testSagaState.setPaymentTransactionId("PAY-TRANS-001");
        testSagaState.setInventoryReserved(true);
        testSagaState.setInventoryTransactionId("INV-TRANS-001");

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        when(communicationStrategy.cancelShipping("SHIPPING-001"))
                .thenReturn(ShippingResponse.builder().success(true).build());
        when(communicationStrategy.refundPayment("PAY-TRANS-001"))
                .thenReturn(PaymentResponse.builder().success(true).build());
        when(communicationStrategy.releaseInventory("INV-TRANS-001"))
                .thenReturn(InventoryResponse.builder().success(true).build());

        // When
        compensationHandler.executeCompensation(testOrder);

        // Then
        verify(communicationStrategy).cancelShipping("SHIPPING-001");
        verify(communicationStrategy).refundPayment("PAY-TRANS-001");
        verify(communicationStrategy).releaseInventory("INV-TRANS-001");
        verify(sagaStateRepository, times(3)).save(any()); // Called for each compensation step
    }

    @Test
    void testExecuteCompensation_PartialWorkflow() {
        // Given
        testSagaState.setShippingArranged(false);
        testSagaState.setPaymentProcessed(true);
        testSagaState.setPaymentTransactionId("PAY-TRANS-001");
        testSagaState.setInventoryReserved(false);

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        when(communicationStrategy.refundPayment("PAY-TRANS-001"))
                .thenReturn(PaymentResponse.builder().success(true).build());

        // When
        compensationHandler.executeCompensation(testOrder);

        // Then
        verify(communicationStrategy, never()).cancelShipping(anyString());
        verify(communicationStrategy).refundPayment("PAY-TRANS-001");
        verify(communicationStrategy, never()).releaseInventory(anyString());
    }

    @Test
    void testExecuteCompensation_ExceptionHandling() {
        // Given
        testSagaState.setShippingArranged(true);
        testSagaState.setShippingTransactionId("SHIPPING-001");
        testSagaState.setPaymentProcessed(true);
        testSagaState.setPaymentTransactionId("PAY-TRANS-001");

        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.of(testSagaState));

        when(communicationStrategy.cancelShipping("SHIPPING-001"))
                .thenThrow(new RuntimeException("Network error"));
        when(communicationStrategy.refundPayment("PAY-TRANS-001"))
                .thenReturn(PaymentResponse.builder().success(true).build());

        // When
        compensationHandler.executeCompensation(testOrder);

        // Then
        verify(communicationStrategy).cancelShipping("SHIPPING-001");
        // Even though first step fails, payment compensation should still be attempted
        verify(communicationStrategy).refundPayment("PAY-TRANS-001");
    }

    @Test
    void testSagaStateNotFound() {
        // Given
        when(sagaStateRepository.findByOrder(testOrder)).thenReturn(Optional.empty());

        // When - exception is caught and logged, method completes without throwing
        compensationHandler.compensateInventory(testOrder);

        // Then - no interactions with communication strategy or repository save
        verify(communicationStrategy, never()).releaseInventory(anyString());
        verify(sagaStateRepository, never()).save(any());
    }
}
