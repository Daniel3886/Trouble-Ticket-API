package org.springboot.troubleticketapi.dto;

public record TroubleTicketSummary(
        String externalId,
        long serviceId,
        String description,
        TroubleTicketStatus status
) {}
