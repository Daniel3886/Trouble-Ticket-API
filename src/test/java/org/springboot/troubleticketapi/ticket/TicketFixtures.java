package org.springboot.troubleticketapi.ticket;

import java.time.Instant;
import java.util.List;

final class TicketFixtures {

    static final String TICKET_ID = "8a56c5af-aeb7-4b6b-b493-25f7e74207f0";
    static final String EXTERNAL_ID = "OK-123456";
    static final long SERVICE_ID = 987654321L;

    private TicketFixtures() {}

    static TroubleTicket ticket(TroubleTicketStatus status) {
        return new TroubleTicket(
                TICKET_ID,
                EXTERNAL_ID,
                SERVICE_ID,
                "No data connection",
                status,
                List.of(new Note("note-1", "Initial note", Instant.parse("2026-01-01T10:00:00Z"))));
    }

    static String createJson() {
        return """
                {
                    "externalId":"%s",
                    "serviceId":%d,
                    "description":"No data connection",
                    "status":"new",
                    "note":"Initial note"
                }
                """.formatted(EXTERNAL_ID, SERVICE_ID);
    }
}
