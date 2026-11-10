package org.springboot.troubleticketapi.ticket;

import jakarta.validation.constraints.NotNull;

record TroubleTicketCloseStatusRequest(@NotNull TroubleTicketCloseStatus status) {}
