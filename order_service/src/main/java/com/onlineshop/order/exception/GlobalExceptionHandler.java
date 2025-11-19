package com.onlineshop.order.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(OrderNotFoundException ex) {
        // TODO: Implement exception handling
        return null;
    }
    
    @ExceptionHandler(SagaException.class)
    public ResponseEntity<ErrorResponse> handleSagaException(SagaException ex) {
        // TODO: Implement exception handling
        return null;
    }
    
    @ExceptionHandler(CompensationException.class)
    public ResponseEntity<ErrorResponse> handleCompensationException(CompensationException ex) {
        // TODO: Implement exception handling
        return null;
    }
    
    @ExceptionHandler(ServiceCommunicationException.class)
    public ResponseEntity<ErrorResponse> handleServiceCommunicationException(ServiceCommunicationException ex) {
        // TODO: Implement exception handling
        return null;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // TODO: Implement validation exception handling
        return null;
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        // TODO: Implement global exception handling
        return null;
    }
}
