package org.springboot.troubleticketapi.entity;

import jakarta.persistence.*;

import java.util.List;

@Table(name = "trouble_tickets")
@Entity
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

    private List<Note> notes;

}
