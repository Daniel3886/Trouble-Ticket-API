package org.springboot.troubleticketapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springboot.troubleticketapi.dto.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Error> handleTroubleTicketNotFound(
            TicketNotFoundException e,
            HttpServletRequest request
    ) {
        Error error = new Error(
                "TROUBLE_TICKET_NOT_FOUND",
                e.getMessage(),
                (String) request.getAttribute("requestId")
        );

        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(error);
    }
}
