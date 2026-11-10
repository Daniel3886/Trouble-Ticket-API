package org.springboot.troubleticketapi.ticket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

enum TroubleTicketCloseStatus {
    CLOSED("closed");

    private final String value;

    TroubleTicketCloseStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TroubleTicketCloseStatus fromValue(String value) {
        if (CLOSED.value.equals(value)) {
            return CLOSED;
        }
        throw new IllegalArgumentException("Close status must be 'closed'");
    }
}
