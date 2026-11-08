package org.springboot.troubleticketapi.dto;

import java.time.Instant;

public record Note(String id, String text, Instant date) {}
