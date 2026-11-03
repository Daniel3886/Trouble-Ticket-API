package org.springboot.troubleticketapi.dto;

public record TroubleTicketSummary(
        String externalId,
        Long serviceId,
        String description,
        TroubleTicketStatus status
) {}
