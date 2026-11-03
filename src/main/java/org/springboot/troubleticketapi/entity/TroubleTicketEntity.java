package org.springboot.troubleticketapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.springboot.troubleticketapi.dto.TroubleTicketStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(
        name = "trouble_tickets",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_tenant_external_id",
                columnNames = {"tenant_id", "external_id"}
        )
)
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
    @Length(max = 255)
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
    @OrderBy("date ASC")
    private List<NoteEntity> notes = new ArrayList<>();

    @Version
    private Long version;

    public TroubleTicketEntity(
            String tenantId,
            String externalId,
            long serviceId,
            String description,
            TroubleTicketStatus status
    ) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.externalId = externalId;
        this.serviceId = serviceId;
        this.description = description;
        this.status = status;
    }

    public NoteEntity addNote(String text) {
        NoteEntity note = new NoteEntity(text, this);
        notes.add(note);
        return note;
    }

    public void close() {
        this.status = TroubleTicketStatus.CLOSED;
    }
}
