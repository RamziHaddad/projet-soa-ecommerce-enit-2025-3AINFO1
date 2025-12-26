package com.enit.catalog.exception;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String method;
    private String correlationId;
    private String errorCode;
    private Map<String, String> details;

    public ErrorResponse() {}

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ErrorResponse instance = new ErrorResponse();
        public Builder timestamp(Instant t) { instance.setTimestamp(t); return this; }
        public Builder status(int s) { instance.setStatus(s); return this; }
        public Builder error(String e) { instance.setError(e); return this; }
        public Builder message(String m) { instance.setMessage(m); return this; }
        public Builder path(String p) { instance.setPath(p); return this; }
        public Builder method(String m) { instance.setMethod(m); return this; }
        public Builder correlationId(String c) { instance.setCorrelationId(c); return this; }
        public Builder errorCode(String e) { instance.setErrorCode(e); return this; }
        public Builder details(Map<String, String> d) { instance.setDetails(d); return this; }
        public ErrorResponse build() { return instance; }
    }
}
