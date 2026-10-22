package org.springboot.troubleticketapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Table(name = "notes")
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Note {

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
    private TroubleTicket troubleTicket;

    public Note(String text, TroubleTicket troubleTicket) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.date = Instant.now();
        this.troubleTicket = troubleTicket;
    }
}
