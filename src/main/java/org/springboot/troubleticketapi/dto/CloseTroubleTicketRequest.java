package org.springboot.troubleticketapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CloseTroubleTicketRequest (
    @NotBlank
    @Pattern(regexp = "closed",
            message = "Status must be 'closed'")
    String status
){}
