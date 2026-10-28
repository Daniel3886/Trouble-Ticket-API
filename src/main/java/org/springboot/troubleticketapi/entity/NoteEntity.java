package org.springboot.troubleticketapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Table(name = "notes")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoteEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 5000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant date;

    @ManyToOne(
            fetch = FetchType.LAZY, optional = false
    )
    @JoinColumn(name = "trouble_ticket_id", nullable = false)
    private TroubleTicketEntity troubleTicket;

    public NoteEntity(String text, TroubleTicketEntity troubleTicket) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.date = Instant.now();
        this.troubleTicket = troubleTicket;
    }
}
