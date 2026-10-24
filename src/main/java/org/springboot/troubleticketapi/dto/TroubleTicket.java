package org.springboot.troubleticketapi.dto;

import java.util.List;

public record TroubleTicket (
        String id,
        String externalId,
        long serviceId,
        String description,
        TroubleTicketStatus status,
        List<Note> notes
){}
