package org.springboot.troubleticketapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteCreateRequest (
        @NotBlank
        @Size(max = 5000, message = "Text must be at most 5000 characters")
        String text
){}
