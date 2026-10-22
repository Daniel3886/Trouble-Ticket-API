package org.springboot.troubleticketapi.dto;

import java.util.List;

public record TroubleTicketResponse (
        String id,
        String externalId,
        Long serviceId,
        String description,
        String status,
        List<NoteResponse> notes
){}
