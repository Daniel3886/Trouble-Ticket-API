package org.springboot.troubleticketapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Table(name = "notes")
@Entity
public class Note {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 5000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant date;

}
