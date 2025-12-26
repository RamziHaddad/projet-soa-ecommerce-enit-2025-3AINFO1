package com.enit.catalog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ErrorResponseFactory {
    public static ErrorResponse from(Exception ex, HttpStatus status, HttpServletRequest request, String errorCode, Map<String, String> details) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .correlationId(correlationId)
                .errorCode(errorCode)
                .details(details)
                .build();
    }
}
