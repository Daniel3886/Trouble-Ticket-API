package org.springboot.troubleticketapi.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteCreateRequest (
        @NotBlank
        String text
){}
