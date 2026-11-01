package org.springboot.troubleticketapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springboot.troubleticketapi.dto.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Error> handleTroubleTicketNotFound(
            TicketNotFoundException e,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "TROUBLE_TICKET_NOT_FOUND",
                e,
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<Error> handleServiceNotFound(
            ServiceNotFoundException e,
            HttpServletRequest request
    ){
        return buildErrorResponse(
                "SERVICE_NOT_FOUND",
                e,
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "VALIDATION_ERROR",
                e,
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(MissingTenantException.class)
    public ResponseEntity<Error> handleMissingTenant(
            MissingTenantException e,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "MISSING_TENANT",
                e,
                HttpStatus.FORBIDDEN,
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Error> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "ILLEGAL_ARGUMENT",
                e,
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> handleUnreadableMessage(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "VALIDATION_ERROR",
                e,
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    private ResponseEntity<Error> buildErrorResponse(
            String errorCode,
            Exception e,
            HttpStatus status,
            HttpServletRequest request
    ) {
        Error error = new Error(
                errorCode,
                e.getMessage(),
                (String) request.getAttribute("requestId")
        );

        return ResponseEntity.
                status(status)
                .body(error);
    }
}
