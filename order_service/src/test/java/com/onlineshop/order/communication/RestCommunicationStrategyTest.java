package com.onlineshop.order.communication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlineshop.order.client.InventoryServiceClient;
import com.onlineshop.order.client.PaymentServiceClient;
import com.onlineshop.order.client.ShippingServiceClient;
import com.onlineshop.order.communication.RestCommunicationStrategy;
import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.InventoryItemRequest;
import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.InventoryResponse;
import com.onlineshop.order.dto.response.PaymentResponse;
import com.onlineshop.order.dto.response.ShippingResponse;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestCommunicationStrategyTest {

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private ShippingServiceClient shippingServiceClient;

    @InjectMocks
    private RestCommunicationStrategy restCommunicationStrategy;

    private InventoryRequest inventoryRequest;
    private PaymentRequest paymentRequest;
    private ShippingRequest shippingRequest;
    private InventoryResponse inventoryResponse;
    private PaymentResponse paymentResponse;
    private ShippingResponse shippingResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        InventoryItemRequest inventoryItem = InventoryItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();
        
        inventoryRequest = InventoryRequest.builder()
                .orderNumber("ORD-2025-001")
                .items(Arrays.asList(inventoryItem))
                .build();
        
        paymentRequest = PaymentRequest.builder()
                .orderNumber("ORD-2025-001")
                .customerId(1L)
                .amount(new BigDecimal("59.98"))
                .paymentMethod("CREDIT_CARD")
                .build();
        
        shippingRequest = ShippingRequest.builder()
                .orderNumber("ORD-2025-001")
                .customerId(1L)
                .shippingAddress("123 Main St, City, State 12345")
                .build();
        
        inventoryResponse = InventoryResponse.builder()
                .success(true)
                .transactionId("INV-TXN-001")
                .message("Inventory reserved successfully")
                .build();
        
        paymentResponse = PaymentResponse.builder()
                .success(true)
                .transactionId("PAY-TXN-001")
                .message("Payment processed successfully")
                .build();
        
        shippingResponse = ShippingResponse.builder()
                .success(true)
                .trackingNumber("SHIP-TRK-001")
                .message("Shipping arranged successfully")
                .build();
    }

    @Test
    void testReserveInventory() {
        // Given
        when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class))).thenReturn(inventoryResponse);

        // When
        InventoryResponse result = restCommunicationStrategy.reserveInventory(inventoryRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("INV-TXN-001", result.getTransactionId());
        assertEquals("Inventory reserved successfully", result.getMessage());
        
        verify(inventoryServiceClient, times(1)).reserveInventory(inventoryRequest);
    }

    @Test
    void testReleaseInventory() {
        // Given
        String transactionId = "INV-TXN-001";
        when(inventoryServiceClient.releaseInventory(transactionId)).thenReturn(inventoryResponse);

        // When
        InventoryResponse result = restCommunicationStrategy.releaseInventory(transactionId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("INV-TXN-001", result.getTransactionId());
        
        verify(inventoryServiceClient, times(1)).releaseInventory(transactionId);
    }

    @Test
    void testProcessPayment() {
        // Given
        when(paymentServiceClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

        // When
        PaymentResponse result = restCommunicationStrategy.processPayment(paymentRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("PAY-TXN-001", result.getTransactionId());
        assertEquals("Payment processed successfully", result.getMessage());
        
        verify(paymentServiceClient, times(1)).processPayment(paymentRequest);
    }

    @Test
    void testRefundPayment() {
        // Given
        String transactionId = "PAY-TXN-001";
        when(paymentServiceClient.refundPayment(transactionId)).thenReturn(paymentResponse);

        // When
        PaymentResponse result = restCommunicationStrategy.refundPayment(transactionId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("PAY-TXN-001", result.getTransactionId());
        
        verify(paymentServiceClient, times(1)).refundPayment(transactionId);
    }

    @Test
    void testArrangeShipping() {
        // Given
        when(shippingServiceClient.arrangeShipping(any(ShippingRequest.class))).thenReturn(shippingResponse);

        // When
        ShippingResponse result = restCommunicationStrategy.arrangeShipping(shippingRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("SHIP-TRK-001", result.getTrackingNumber());
        assertEquals("Shipping arranged successfully", result.getMessage());
        
        verify(shippingServiceClient, times(1)).arrangeShipping(shippingRequest);
    }

    @Test
    void testCancelShipping() {
        // Given
        String trackingNumber = "SHIP-TRK-001";
        when(shippingServiceClient.cancelShipping(trackingNumber)).thenReturn(shippingResponse);

        // When
        ShippingResponse result = restCommunicationStrategy.cancelShipping(trackingNumber);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("SHIP-TRK-001", result.getTrackingNumber());
        
        verify(shippingServiceClient, times(1)).cancelShipping(trackingNumber);
    }

    @Test
    void testCommunicationFailure() {
        // Given
        String errorMessage = "Service unavailable";
        when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                .thenThrow(new RuntimeException(errorMessage));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            restCommunicationStrategy.reserveInventory(inventoryRequest);
        });
        
        verify(inventoryServiceClient, times(1)).reserveInventory(inventoryRequest);
    }

    @Test
    void testInventoryServiceFailureWithFallback() {
        // Given
        String errorMessage = "Circuit breaker open";
        when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                .thenThrow(new RuntimeException(errorMessage));

        // When - Using reflection to call the fallback method directly
        InventoryResponse fallbackResult = restCommunicationStrategy
                .fallbackReserveInventory(inventoryRequest, new RuntimeException(errorMessage));

        // Then
        assertNotNull(fallbackResult);
        assertFalse(fallbackResult.getSuccess());
        assertTrue(fallbackResult.getMessage().contains("Service temporarily unavailable"));
        assertTrue(fallbackResult.getMessage().contains(errorMessage));
    }

    @Test
    void testPaymentServiceFailureWithFallback() {
        // Given
        String errorMessage = "Payment gateway timeout";
        when(paymentServiceClient.processPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException(errorMessage));

        // When - Using reflection to call the fallback method directly
        PaymentResponse fallbackResult = restCommunicationStrategy
                .fallbackProcessPayment(paymentRequest, new RuntimeException(errorMessage));

        // Then
        assertNotNull(fallbackResult);
        assertFalse(fallbackResult.getSuccess());
        assertTrue(fallbackResult.getMessage().contains("Service temporarily unavailable"));
        assertTrue(fallbackResult.getMessage().contains(errorMessage));
    }

    @Test
    void testShippingServiceFailureWithFallback() {
        // Given
        String errorMessage = "Shipping service unavailable";
        when(shippingServiceClient.arrangeShipping(any(ShippingRequest.class)))
                .thenThrow(new RuntimeException(errorMessage));

        // When - Using reflection to call the fallback method directly
        ShippingResponse fallbackResult = restCommunicationStrategy
                .fallbackArrangeShipping(shippingRequest, new RuntimeException(errorMessage));

        // Then
        assertNotNull(fallbackResult);
        assertFalse(fallbackResult.getSuccess());
        assertTrue(fallbackResult.getMessage().contains("Service temporarily unavailable"));
        assertTrue(fallbackResult.getMessage().contains(errorMessage));
    }

    @Test
    void testInventoryReleaseFailureWithFallback() {
        // Given
        String errorMessage = "Inventory release failed";
        String transactionId = "INV-TXN-001";
        when(inventoryServiceClient.releaseInventory(transactionId))
                .thenThrow(new RuntimeException(errorMessage));

        // When - Using reflection to call the fallback method directly
        InventoryResponse fallbackResult = restCommunicationStrategy
                .fallbackReleaseInventory(transactionId, new RuntimeException(errorMessage));

        // Then
        assertNotNull(fallbackResult);
        assertFalse(fallbackResult.getSuccess());
        assertTrue(fallbackResult.getMessage().contains("Service temporarily unavailable"));
    }

    @Test
    void testPaymentRefundFailureWithFallback() {
        // Given
        String errorMessage = "Payment refund failed";
        String transactionId = "PAY-TXN-001";
        when(paymentServiceClient.refundPayment(transactionId))
                .thenThrow(new RuntimeException(errorMessage));

        // When - Using reflection to call the fallback method directly
        PaymentResponse fallbackResult = restCommunicationStrategy
                .fallbackRefundPayment(transactionId, new RuntimeException(errorMessage));

        // Then
        assertNotNull(fallbackResult);
        assertFalse(fallbackResult.getSuccess());
        assertTrue(fallbackResult.getMessage().contains("Service temporarily unavailable"));
    }

    @Test
    void testShippingCancellationFailureWithFallback() {
        // Given
        String errorMessage = "Shipping cancellation failed";
        String trackingNumber = "SHIP-TRK-001";
        when(shippingServiceClient.cancelShipping(trackingNumber))
                .thenThrow(new RuntimeException(errorMessage));

        // When - Using reflection to call the fallback method directly
        ShippingResponse fallbackResult = restCommunicationStrategy
                .fallbackCancelShipping(trackingNumber, new RuntimeException(errorMessage));

        // Then
        assertNotNull(fallbackResult);
        assertFalse(fallbackResult.getSuccess());
        assertTrue(fallbackResult.getMessage().contains("Service temporarily unavailable"));
    }
}
