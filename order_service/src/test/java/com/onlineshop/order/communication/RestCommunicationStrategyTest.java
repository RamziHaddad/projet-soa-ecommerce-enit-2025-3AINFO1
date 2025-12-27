package com.onlineshop.order.communication;

import com.onlineshop.order.client.InventoryServiceClient;
import com.onlineshop.order.client.PaymentServiceClient;
import com.onlineshop.order.client.ShippingServiceClient;
import com.onlineshop.order.dto.request.InventoryRequest;
import com.onlineshop.order.dto.request.InventoryItemRequest;
import com.onlineshop.order.dto.request.PaymentRequest;
import com.onlineshop.order.dto.request.ShippingRequest;
import com.onlineshop.order.dto.response.DeliveryResponse;
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
        private DeliveryResponse deliveryResponse;

        @BeforeEach
        void setUp() {

                InventoryItemRequest inventoryItem = new InventoryItemRequest("PROD-001", 2);

                inventoryRequest = new InventoryRequest("ORD-2025-001", Arrays.asList(inventoryItem));

                paymentRequest = new PaymentRequest("ORD-2025-001", 1L, new BigDecimal("59.98"), "CREDIT_CARD");

                shippingRequest = new ShippingRequest("ORD-2025-001", 1L, "123 Main St, City, State 12345");

                inventoryResponse = new InventoryResponse(true, "INV-TXN-001", "Inventory reserved successfully",
                                java.util.List.of());

                paymentResponse = new PaymentResponse(true, "PAY-TXN-001", "Payment processed successfully", null,
                                null);

                shippingResponse = new ShippingResponse(true, "SHIP-TRK-001", "Shipping arranged successfully", null,
                                null);
                deliveryResponse = new DeliveryResponse(1L, 1L, 1L, "PENDING", "SHIP-TRK-001", null);
               
        }

        @Test
        void testReserveInventory() {

                when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                                .thenReturn(inventoryResponse);

                InventoryResponse result = restCommunicationStrategy.reserveInventory(inventoryRequest);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals("INV-TXN-001", result.orderId());
                assertEquals("Inventory reserved successfully", result.message());

                verify(inventoryServiceClient, times(1)).reserveInventory(inventoryRequest);
        }

        @Test
        void testReleaseInventory() {

                String orderId = "ORD-2025-001";
                doNothing().when(inventoryServiceClient).cancelReservation(orderId);

                InventoryResponse result = restCommunicationStrategy.releaseInventory(orderId);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals(orderId, result.orderId());
                assertEquals("Inventory reservation cancelled successfully", result.message());

                verify(inventoryServiceClient, times(1)).cancelReservation(orderId);
        }

        @Test
        void testProcessPayment() {

                when(paymentServiceClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

                PaymentResponse result = restCommunicationStrategy.processPayment(paymentRequest);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals("PAY-TXN-001", result.transactionId());
                assertEquals("Payment processed successfully", result.message());

                verify(paymentServiceClient, times(1)).processPayment(paymentRequest);
        }

        @Test
        void testRefundPayment() {

                String transactionId = "PAY-TXN-001";
                when(paymentServiceClient.refundPayment(transactionId)).thenReturn(paymentResponse);

                PaymentResponse result = restCommunicationStrategy.refundPayment(transactionId);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals("PAY-TXN-001", result.transactionId());

                verify(paymentServiceClient, times(1)).refundPayment(transactionId);
        }

        @Test
        void testArrangeShipping() {

                when(shippingServiceClient.arrangeShipping(any(ShippingRequest.class))).thenReturn(deliveryResponse);

                ShippingResponse result = restCommunicationStrategy.arrangeShipping(shippingRequest);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals("SHIP-TRK-001", result.trackingNumber());
                assertEquals("Shipping arranged successfully", result.message());

                verify(shippingServiceClient, times(1)).arrangeShipping(shippingRequest);
        }

        @Test
        void testCancelShipping() {

                String trackingNumber = "SHIP-TRK-001";
                when(shippingServiceClient.cancelShipping(trackingNumber)).thenReturn(shippingResponse);

                ShippingResponse result = restCommunicationStrategy.cancelShipping(trackingNumber);

                assertNotNull(result);
                assertTrue(result.success());
                assertEquals("SHIP-TRK-001", result.trackingNumber());

                verify(shippingServiceClient, times(1)).cancelShipping(trackingNumber);
        }

        @Test
        void testCommunicationFailure() {

                String errorMessage = "Service unavailable";
                when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                InventoryResponse result = restCommunicationStrategy.reserveInventory(inventoryRequest);

                assertNotNull(result);
                assertFalse(result.success());
                assertTrue(result.message().toLowerCase().contains("inventory service temporarily unavailable"));
        }

        @Test
        void testInventoryServiceFailureWithFallback() {

                String errorMessage = "service temporarily unavailable";
                when(inventoryServiceClient.reserveInventory(any(InventoryRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                InventoryResponse result = restCommunicationStrategy.reserveInventory(inventoryRequest);

                assertNotNull(result);
                assertFalse(result.success());
                assertTrue(result.message().toLowerCase().contains("inventory service temporarily unavailable"));
        }

        @Test
        void testPaymentServiceFailureWithFallback() {

                String errorMessage = "service temporarily unavailable";
                when(paymentServiceClient.processPayment(any(PaymentRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                PaymentResponse result = restCommunicationStrategy.processPayment(paymentRequest);

                assertNotNull(result);
                assertFalse(result.success());
                assertTrue(result.message().toLowerCase().contains("payment service temporarily unavailable"));
        }

        @Test
        void testShippingServiceFailureWithFallback() {

                String errorMessage = "service temporarily unavailable";
                when(shippingServiceClient.arrangeShipping(any(ShippingRequest.class)))
                                .thenThrow(new RuntimeException(errorMessage));

                ShippingResponse result = restCommunicationStrategy.arrangeShipping(shippingRequest);

                assertNotNull(result);
                assertFalse(result.success());
                assertTrue(result.message().toLowerCase().contains("shipping service temporarily unavailable"));
        }

}
