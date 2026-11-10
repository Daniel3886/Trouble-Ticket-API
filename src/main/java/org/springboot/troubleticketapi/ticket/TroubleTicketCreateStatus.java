package org.springboot.troubleticketapi.ticket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

enum TroubleTicketCreateStatus {
    NEW("new");

    private final String value;

    TroubleTicketCreateStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TroubleTicketCreateStatus fromValue(String value) {
        if (NEW.value.equals(value)) {
            return NEW;
        }
        throw new IllegalArgumentException("Create status must be 'new'");
    }
}
