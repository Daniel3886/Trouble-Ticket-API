package org.springboot.troubleticketapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springboot.troubleticketapi.dto.Error;
import org.springboot.troubleticketapi.security.RequestIdFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Error> handleTroubleTicketNotFound(TicketNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "TROUBLE_TICKET_NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<Error> handleServiceNotFound(ServiceNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> handleMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", unreadableBodyMessage(e), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", validationMessage(e), request);
    }

    @ExceptionHandler(MissingTenantException.class)
    public ResponseEntity<Error> handleMissingTenant(MissingTenantException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "MISSING_TENANT", e.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Error> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", e.getMessage(), request);
    }

    private ResponseEntity<Error> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new Error(code, message, (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }

    private String unreadableBodyMessage(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof UnrecognizedPropertyException unknown) {
            return "Field '" + unknown.getPropertyName() + "' is not allowed";
        }
        if (e.getCause() instanceof JacksonException jackson) {
            String field = jackson.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            if (!field.isEmpty()) {
                return "Field '" + field + "' has an invalid value";
            }
        }
        return "Malformed or invalid request body";
    }

    private String validationMessage(MethodArgumentNotValidException e) {
        StringBuilder message = new StringBuilder();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            if (!message.isEmpty()) {
                message.append("; ");
            }
            message.append("Field '").append(fieldError.getField()).append("' ").append(fieldError.getDefaultMessage());
        }
        return message.toString();
    }
}
