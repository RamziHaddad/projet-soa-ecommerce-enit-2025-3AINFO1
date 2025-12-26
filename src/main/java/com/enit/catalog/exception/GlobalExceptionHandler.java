package com.enit.catalog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponseFactory.from(
                ex,
                HttpStatus.NOT_FOUND,
                request,
                ex.getErrorCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponseFactory.from(
                ex,
                HttpStatus.CONFLICT,
                request,
                ex.getErrorCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponseFactory.from(
                ex,
                HttpStatus.BAD_REQUEST,
                request,
                ex.getErrorCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage(),
                        (a, b) -> a
                ));
        ErrorResponse body = ErrorResponseFactory.from(
                ex,
                HttpStatus.BAD_REQUEST,
                request,
                ErrorCode.INVALID_REQUEST.name(),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
                ErrorResponse body = ErrorResponseFactory.from(
                                ex,
                                HttpStatus.FORBIDDEN,
                                request,
                                ErrorCode.ACCESS_DENIED.name(),
                                null
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
                ErrorResponse body = ErrorResponseFactory.from(
                                ex,
                                HttpStatus.CONFLICT,
                                request,
                                ErrorCode.DATA_INTEGRITY_VIOLATION.name(),
                                null
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
                ErrorResponse body = ErrorResponseFactory.from(
                                ex,
                                HttpStatus.PAYLOAD_TOO_LARGE,
                                request,
                                ErrorCode.MAX_UPLOAD_SIZE_EXCEEDED.name(),
                                null
                );
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
        }

    @ExceptionHandler(GlobalAppException.class)
    public ResponseEntity<ErrorResponse> handleGlobalApp(GlobalAppException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponseFactory.from(
                ex,
                ex.getHttpStatus(),
                request,
                ex.getErrorCode().name(),
                null
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponseFactory.from(
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                ErrorCode.GENERIC_ERROR.name(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
