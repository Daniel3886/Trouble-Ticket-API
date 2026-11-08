package org.springboot.troubleticketapi.dto;

import java.util.List;

public record TroubleTicket(
        String id,
        String externalId,
        Long serviceId,
        String description,
        TroubleTicketStatus status,
        List<Note> notes) {}
