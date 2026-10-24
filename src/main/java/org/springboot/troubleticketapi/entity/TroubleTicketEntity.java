package org.springboot.troubleticketapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springboot.troubleticketapi.dto.TroubleTicketStatus;

import java.util.ArrayList;
import java.util.List;

@Table(name = "trouble_tickets")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class TroubleTicketEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "external_id", nullable = false, updatable = false)
    private String externalId;

    @Column(name = "service_id", nullable = false)
    private long serviceId;

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
    private List<NoteEntity> notes = new ArrayList<>();


    public TroubleTicketEntity(
            String id,
            String tenantId,
            String externalId,
            long serviceId,
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
        NoteEntity note = new NoteEntity(text, this);
        notes.add(note);
    }

    public void close() {
        this.status = TroubleTicketStatus.CLOSED;
    }
}
