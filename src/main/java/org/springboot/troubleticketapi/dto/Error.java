package org.springboot.troubleticketapi.dto;

public record Error (
        String code,
        String message,
        String requestId
){}
