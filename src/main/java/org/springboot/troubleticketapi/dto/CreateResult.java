package org.springboot.troubleticketapi.dto;

public record CreateResult(
        TroubleTicket ticket,
        boolean created
) {}
