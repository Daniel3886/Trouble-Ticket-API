package org.springboot.troubleticketapi.ticket;

record TroubleTicketSummary(String externalId, Long serviceId, String description, TroubleTicketStatus status) {}
