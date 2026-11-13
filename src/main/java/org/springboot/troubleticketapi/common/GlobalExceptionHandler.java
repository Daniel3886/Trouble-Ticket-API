package org.springboot.troubleticketapi.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springboot.troubleticketapi.security.MissingTenantException;
import org.springboot.troubleticketapi.servicecatalog.ServiceNotFoundException;
import org.springboot.troubleticketapi.ticket.InvalidStatusException;
import org.springboot.troubleticketapi.ticket.TicketNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Error> handleTroubleTicketNotFound(TicketNotFoundException e, HttpServletRequest request) {
        return error(ErrorCode.TROUBLE_TICKET_NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<Error> handleServiceNotFound(ServiceNotFoundException e, HttpServletRequest request) {
        return error(ErrorCode.SERVICE_NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<Error> handleInvalidStatus(InvalidStatusException e, HttpServletRequest request) {
        return error(ErrorCode.VALIDATION_ERROR, e.getMessage(), request);
    }

    @ExceptionHandler(MissingTenantException.class)
    public ResponseEntity<Error> handleMissingTenant(MissingTenantException e, HttpServletRequest request) {
        return error(ErrorCode.MISSING_TENANT, e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> handleMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        return error(ErrorCode.VALIDATION_ERROR, unreadableBodyMessage(e), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        return error(ErrorCode.VALIDATION_ERROR, validationMessage(e), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Error> handleConcurrentModification(
            OptimisticLockingFailureException e, HttpServletRequest request) {
        return error(ErrorCode.CONFLICT, ErrorCode.CONFLICT.defaultMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleUnexpected(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse springError) {
            ErrorCode code = switch (springError.getStatusCode().value()) {
                case 404 -> ErrorCode.NOT_FOUND;
                case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
                case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
                default -> ErrorCode.VALIDATION_ERROR;
            };
            return error(code, code.defaultMessage(), request);
        }

        log.error("Unexpected error, requestId={}", requestId(request), e);
        return error(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), request);
    }

    private ResponseEntity<Error> error(ErrorCode code, String message, HttpServletRequest request) {
        return ResponseEntity.status(code.status()).body(new Error(code.name(), message, requestId(request)));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
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
