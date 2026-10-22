package org.springboot.troubleticketapi.dto;

import java.time.Instant;

public record NoteResponse (
        String id,
        String note,
        Instant date
){}
