package com.onlineshop.order.communication;

import com.onlineshop.order.client.InventoryServiceClient;
import com.onlineshop.order.client.PaymentServiceClient;
import com.onlineshop.order.client.ShippingServiceClient;
import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.InventoryItemRequest;
import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.InventoryResponse;
import com.onlineshop.order.dto.response.PaymentResponse;
import com.onlineshop.order.dto.response.ShippingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class RestCommunicationStrategyTest {

        @Autowired
        private RestCommunicationStrategy restCommunicationStrategy;

        @MockBean
        private InventoryServiceClient inventoryServiceClient;

        @MockBean
        private PaymentServiceClient paymentServiceClient;

        @MockBean
        private ShippingServiceClient shippingServiceClient;

        private InventoryRequest inventoryRequest;
        private PaymentRequest paymentRequest;
        private ShippingRequest shippingRequest;
        private InventoryResponse inventoryResponse;
        private PaymentResponse paymentResponse;
        private ShippingResponse shippingResponse;

        @BeforeEach
        void setUp() {

                InventoryItemRequest inventoryItem = InventoryItemRequest.builder()
                                .productId("PROD-001")
                                .quantity(2)
                                .build();

inventoryRequest = InventoryRequest.builder()
                                .orderId("ORD-2025-001")
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
                                .orderId("INV-TXN-001")
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

                when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                                .thenReturn(inventoryResponse);

                InventoryResponse result = restCommunicationStrategy.reserveInventory(inventoryRequest);

                assertNotNull(result);
assertTrue(result.isSuccess());
assertEquals("INV-TXN-001", result.getOrderId());
                assertEquals("Inventory reserved successfully", result.getMessage());

                verify(inventoryServiceClient, times(1)).reserveInventory(inventoryRequest);
        }

@Test
void testReleaseInventory() {

        String orderId = "ORD-2025-001";
        doNothing().when(inventoryServiceClient).cancelReservation(orderId);

        InventoryResponse result = restCommunicationStrategy.releaseInventory(orderId);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(orderId, result.getOrderId());
        assertEquals("Inventory reservation cancelled successfully", result.getMessage());

        verify(inventoryServiceClient, times(1)).cancelReservation(orderId);
}

        @Test
        void testProcessPayment() {

                when(paymentServiceClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

                PaymentResponse result = restCommunicationStrategy.processPayment(paymentRequest);

                assertNotNull(result);
                assertTrue(result.getSuccess());
                assertEquals("PAY-TXN-001", result.getTransactionId());
                assertEquals("Payment processed successfully", result.getMessage());

                verify(paymentServiceClient, times(1)).processPayment(paymentRequest);
        }

        @Test
        void testRefundPayment() {

                String transactionId = "PAY-TXN-001";
                when(paymentServiceClient.refundPayment(transactionId)).thenReturn(paymentResponse);

                PaymentResponse result = restCommunicationStrategy.refundPayment(transactionId);

                assertNotNull(result);
                assertTrue(result.getSuccess());
                assertEquals("PAY-TXN-001", result.getTransactionId());

                verify(paymentServiceClient, times(1)).refundPayment(transactionId);
        }

        @Test
        void testArrangeShipping() {

                when(shippingServiceClient.arrangeShipping(any(ShippingRequest.class))).thenReturn(shippingResponse);

                ShippingResponse result = restCommunicationStrategy.arrangeShipping(shippingRequest);

                assertNotNull(result);
                assertTrue(result.getSuccess());
                assertEquals("SHIP-TRK-001", result.getTrackingNumber());
                assertEquals("Shipping arranged successfully", result.getMessage());

                verify(shippingServiceClient, times(1)).arrangeShipping(shippingRequest);
        }

        @Test
        void testCancelShipping() {

                String trackingNumber = "SHIP-TRK-001";
                when(shippingServiceClient.cancelShipping(trackingNumber)).thenReturn(shippingResponse);

                ShippingResponse result = restCommunicationStrategy.cancelShipping(trackingNumber);

                assertNotNull(result);
                assertTrue(result.getSuccess());
                assertEquals("SHIP-TRK-001", result.getTrackingNumber());

                verify(shippingServiceClient, times(1)).cancelShipping(trackingNumber);
        }

        @Test
        void testCommunicationFailure() {

                String errorMessage = "Service unavailable";
                when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                InventoryResponse result = restCommunicationStrategy.reserveInventory(inventoryRequest);

                assertNotNull(result);
assertFalse(result.isSuccess());
                assertTrue(result.getMessage().toLowerCase().contains("inventory service temporarily unavailable"));
        }

        @Test
        void testInventoryServiceFailureWithFallback() {

                String errorMessage = "service temporarily unavailable";
                when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                InventoryResponse result = restCommunicationStrategy.reserveInventory(inventoryRequest);

                assertNotNull(result);
assertFalse(result.isSuccess());
                assertTrue(result.getMessage().toLowerCase().contains("inventory service temporarily unavailable"));
        }

        @Test
        void testPaymentServiceFailureWithFallback() {

                String errorMessage = "service temporarily unavailable";
                when(paymentServiceClient.processPayment(any(PaymentRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                PaymentResponse result = restCommunicationStrategy.processPayment(paymentRequest);

                assertNotNull(result);
                assertFalse(result.getSuccess());
                assertTrue(result.getMessage().toLowerCase().contains("payment service temporarily unavailable"));
        }

        @Test
        void testShippingServiceFailureWithFallback() {

                String errorMessage = "service temporarily unavailable";
                when(shippingServiceClient.arrangeShipping(any(ShippingRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                ShippingResponse result = restCommunicationStrategy.arrangeShipping(shippingRequest);

                assertNotNull(result);
                assertFalse(result.getSuccess());
                assertTrue(result.getMessage().toLowerCase().contains("shipping service temporarily unavailable"));
        }

}
