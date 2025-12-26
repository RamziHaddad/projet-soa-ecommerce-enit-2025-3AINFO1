package com.enit.catalog.exception;

public class InvalidRequestException extends RuntimeException {
    private final String errorCode = ErrorCode.INVALID_REQUEST.name();
    public InvalidRequestException(String message) { super(message); }
    public InvalidRequestException(String message, Throwable cause) { super(message, cause); }
    public String getErrorCode() { return errorCode; }
}
