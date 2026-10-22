package org.springboot.troubleticketapi.dto;

public record ApiErrorResponse (
        String code,
        String message
){}
