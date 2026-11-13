package org.springboot.troubleticketapi.ticket;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "notes")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class NoteEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 5000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trouble_ticket_id", nullable = false)
    private TroubleTicketEntity troubleTicket;

    public NoteEntity(String text, TroubleTicketEntity troubleTicket) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.date = Instant.now();
        this.troubleTicket = troubleTicket;
    }
}
