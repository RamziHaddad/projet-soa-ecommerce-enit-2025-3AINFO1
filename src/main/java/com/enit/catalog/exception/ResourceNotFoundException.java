package com.enit.catalog.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String errorCode = ErrorCode.RESOURCE_NOT_FOUND.name();
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String message, Throwable cause) { super(message, cause); }
    public String getErrorCode() { return errorCode; }
}
