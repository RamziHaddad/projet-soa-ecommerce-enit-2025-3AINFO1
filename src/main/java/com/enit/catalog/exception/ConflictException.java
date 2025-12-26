package com.enit.catalog.exception;

public class ConflictException extends RuntimeException {
    private final String errorCode = ErrorCode.CONFLICT.name();
    public ConflictException(String message) { super(message); }
    public ConflictException(String message, Throwable cause) { super(message, cause); }
    public String getErrorCode() { return errorCode; }
}
