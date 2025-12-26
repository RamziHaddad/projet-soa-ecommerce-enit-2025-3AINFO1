package com.enit.catalog.exception;

import org.springframework.http.HttpStatus;

public class GlobalAppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public GlobalAppException(String message, ErrorCode errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    public GlobalAppException(String message, Throwable cause, ErrorCode errorCode, HttpStatus httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    public ErrorCode getErrorCode() { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
