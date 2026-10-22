package org.springboot.troubleticketapi.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest (
        @NotBlank
        String note
){}
