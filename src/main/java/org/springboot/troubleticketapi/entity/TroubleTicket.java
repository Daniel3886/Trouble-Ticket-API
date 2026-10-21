package org.springboot.troubleticketapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Table(name = "trouble_tickets")
@Entity
@Getter
@NoArgsConstructor
public class TroubleTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "external_id", nullable = false, updatable = false)
    private String externalId;

    @Column(name = "service_id", nullable = false)
    private int serviceId;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TroubleTicketStatus status;

    @OneToMany(
        mappedBy = "troubleTicket",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Note> notes = new ArrayList<>();


    public TroubleTicket(
            String id,
            String tenantId,
            String externalId,
            int serviceId,
            String description,
            TroubleTicketStatus status
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.externalId = externalId;
        this.serviceId = serviceId;
        this.description = description;
        this.status = status;
    }

    public void addNote(String text) {
        Note note = new Note(text, this);
        notes.add(note);
    }

    public void close() {
        this.status = TroubleTicketStatus.CLOSED;
    }
}
