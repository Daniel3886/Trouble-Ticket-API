package org.springboot.troubleticketapi.ticket;

import java.util.List;

record TroubleTicket(
        String id,
        String externalId,
        Long serviceId,
        String description,
        TroubleTicketStatus status,
        List<Note> notes) {}
