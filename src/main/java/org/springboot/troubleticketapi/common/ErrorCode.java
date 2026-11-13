package org.springboot.troubleticketapi.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request is invalid"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Missing or invalid Bearer token"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied"),
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Service not found"),
    TROUBLE_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "Trouble ticket not found"),

    MISSING_TENANT(HttpStatus.FORBIDDEN, "Token doesn't contain tenant_id"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this resource"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type not supported, use application/json"),
    CONFLICT(HttpStatus.CONFLICT, "The trouble ticket was modified concurrently, retry the request"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, quote the requestId when reporting it");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
