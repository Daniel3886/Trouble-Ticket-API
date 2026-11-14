package org.springboot.troubleticketapi.ticket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

enum TroubleTicketStatus {
    NEW("new"),
    ACKNOWLEDGED("acknowledged"),
    IN_PROGRESS("inProgress"),
    RESOLVED("resolved"),
    CLOSED("closed"),
    REJECTED("rejected");

    private final String value;

    TroubleTicketStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TroubleTicketStatus fromValue(String value) {
        for (TroubleTicketStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown trouble ticket status: " + value);
    }
}
