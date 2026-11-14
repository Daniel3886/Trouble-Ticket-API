package org.springboot.troubleticketapi.common;

public record Error(String code, String message, String requestId) {}
