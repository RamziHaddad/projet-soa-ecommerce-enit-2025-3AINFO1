package com.onlineshop.order.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleOrderNotFoundException() {
        // Given
        String errorMessage = "Order not found with ID: 123";
        OrderNotFoundException exception = new OrderNotFoundException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOrderNotFoundException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
        assertEquals("Order Not Found", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testHandleSagaException() {
        // Given
        String errorMessage = "SAGA execution failed at payment step";
        SagaException exception = new SagaException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleSagaException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus());
        assertEquals("SAGA Processing Error", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testHandleCompensationException() {
        // Given
        String errorMessage = "Failed to compensate inventory reservation";
        CompensationException exception = new CompensationException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCompensationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus());
        assertEquals("Compensation Error", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testHandleServiceCommunicationException() {
        // Given
        String errorMessage = "Unable to communicate with inventory service";
        ServiceCommunicationException exception = new ServiceCommunicationException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceCommunicationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), errorResponse.getStatus());
        assertEquals("Service Communication Error", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testHandleIllegalStateException() {
        // Given
        String errorMessage = "Cannot cancel order with status: COMPLETED";
        IllegalStateException exception = new IllegalStateException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalStateException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.CONFLICT.value(), errorResponse.getStatus());
        assertEquals("Illegal State", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testHandleRuntimeException() {
        // Given
        String errorMessage = "Unexpected runtime error occurred";
        RuntimeException exception = new RuntimeException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testHandleGlobalException() {
        // Given
        Exception exception = new Exception("Unexpected system error");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("An unexpected error occurred", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testHandleValidationExceptions() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        FieldError fieldError1 = new FieldError("objectName", "customerId", "Customer ID is required");
        FieldError fieldError2 = new FieldError("objectName", "shippingAddress", "Shipping address cannot be empty");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.Arrays.asList(fieldError1, fieldError2));

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, String> errors = response.getBody();
        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertEquals("Customer ID is required", errors.get("customerId"));
        assertEquals("Shipping address cannot be empty", errors.get("shippingAddress"));
    }

    @Test
    void testHandleValidationExceptionsWithEmptyErrors() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.Collections.emptyList());

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, String> errors = response.getBody();
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testErrorResponseStructure() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(HttpStatus.NOT_FOUND.value())
                .error("Test Error")
                .message("Test error message")
                .path("/api/orders/123")
                .build();

        // Then
        assertNotNull(errorResponse.getTimestamp());
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
        assertEquals("Test Error", errorResponse.getError());
        assertEquals("Test error message", errorResponse.getMessage());
        assertEquals("/api/orders/123", errorResponse.getPath());
    }

    @Test
    void testHandleOrderNotFoundExceptionWithNullMessage() {
        // Given
        OrderNotFoundException exception = new OrderNotFoundException((String) null);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOrderNotFoundException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
        assertEquals("Order Not Found", errorResponse.getError());
        assertNull(errorResponse.getMessage()); // Should be null if exception message is null
    }

    @Test
    void testHandleSagaExceptionWithNestedException() {
        // Given
        RuntimeException cause = new RuntimeException("Network timeout");
        String errorMessage = "SAGA failed due to network issue";
        SagaException exception = new SagaException(errorMessage, cause);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleSagaException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus());
        assertEquals("SAGA Processing Error", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
    }

    @Test
    void testHandleServiceCommunicationExceptionWithNullMessage() {
        // Given
        ServiceCommunicationException exception = new ServiceCommunicationException(null);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceCommunicationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), errorResponse.getStatus());
        assertEquals("Service Communication Error", errorResponse.getError());
        assertNull(errorResponse.getMessage());
    }

    @Test
    void testHandleValidationExceptionsWithNullBindingResult() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(null);

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, String> errors = response.getBody();
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testExceptionHandlerResponseTiming() {
        // Given
        OrderNotFoundException exception = new OrderNotFoundException("Test order not found");

        // When
        long beforeTime = System.currentTimeMillis();
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOrderNotFoundException(exception);
        long afterTime = System.currentTimeMillis();

        // Then
        assertNotNull(response);
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getTimestamp());
        
        // Verify timestamp is recent (within 1 second)
        long timestampMillis = errorResponse.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertTrue(timestampMillis >= beforeTime && timestampMillis <= afterTime + 1000);
    }
}
