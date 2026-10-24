package org.springboot.troubleticketapi.dto;

import jakarta.validation.constraints.NotNull;

public record TroubleTicketCloseStatusRequest (
    @NotNull
    TroubleTicketCloseStatus status
){}
