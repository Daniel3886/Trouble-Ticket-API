package org.springboot.troubleticketapi.ticket;

import java.time.Instant;

record Note(String id, String text, Instant date) {}
