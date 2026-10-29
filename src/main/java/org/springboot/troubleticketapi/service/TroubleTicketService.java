package org.springboot.troubleticketapi.service;

import jakarta.validation.Valid;
import org.springboot.troubleticketapi.dto.Note;
import org.springboot.troubleticketapi.dto.NoteCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicket;
import org.springboot.troubleticketapi.dto.TroubleTicketCloseStatusRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateStatus;
import org.springboot.troubleticketapi.dto.TroubleTicketCloseStatus;
import org.springboot.troubleticketapi.dto.TroubleTicketStatus;
import org.springboot.troubleticketapi.dto.TroubleTicketSummary;
import org.springboot.troubleticketapi.entity.NoteEntity;
import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
import org.springboot.troubleticketapi.exception.TicketNotFoundException;
import org.springboot.troubleticketapi.repository.TroubleTicketRepository;
import org.springboot.troubleticketapi.security.CurrentTenant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TroubleTicketService {

    private final TroubleTicketRepository troubleTicketRepository;
    private final CurrentTenant currentTenant;

    public TroubleTicketService(
            TroubleTicketRepository troubleTicketRepository,
            CurrentTenant currentTenant
    ) {
        this.troubleTicketRepository = troubleTicketRepository;
        this.currentTenant = currentTenant;
    }

    public TroubleTicket createTroubleTicket(TroubleTicketCreateRequest request) {

        String tenantId = currentTenant.getTenantId();

        if (request.status() != TroubleTicketCreateStatus.NEW) {
            throw new IllegalArgumentException("New tickets must have status 'new'");
        }

        Optional<TroubleTicketEntity> existing =
                troubleTicketRepository.findByTenantIdAndExternalId(tenantId, request.externalId());
        if (existing.isPresent()) {
            return toTroubleTicket(existing.get());
        }

        TroubleTicketStatus status =
                TroubleTicketStatus.NEW;

        TroubleTicketEntity troubleTicket = new TroubleTicketEntity(
                tenantId,
                request.externalId(),
                request.serviceId(),
                request.description(),
                status
        );

        troubleTicket.addNote(request.note());

        TroubleTicketEntity savedTicket = troubleTicketRepository.save(troubleTicket);

        return toTroubleTicket(savedTicket);
    }



    public List<TroubleTicketSummary> getTroubleAllTickets() {
        String tenantId = currentTenant.getTenantId();

        return troubleTicketRepository.findAllByTenantId(tenantId)
                .stream()
                .map(ticket -> new TroubleTicketSummary(
                        ticket.getExternalId(),
                        ticket.getServiceId(),
                        ticket.getDescription(),
                        ticket.getStatus()
                ))
                .toList();
    }

    public TroubleTicket getTroubleTicketById(String id) {
        String tenantId = currentTenant.getTenantId();

        TroubleTicketEntity ticket = troubleTicketRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(
                        () -> new TicketNotFoundException("Ticket not found with id: " + id)
                );
        return toTroubleTicket(ticket);
    }

    public TroubleTicket closeTroubleTicket(
            String id,
            TroubleTicketCloseStatusRequest request
    ) {
        String tenantId = currentTenant.getTenantId();

        if (request.status() != TroubleTicketCloseStatus.CLOSED) {
            throw new TicketNotFoundException("Trouble tickets can only be closed with status 'closed'");
        }

        TroubleTicketEntity ticket = troubleTicketRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TicketNotFoundException("Trouble ticket not found: " + id));

        ticket.close();
        return toTroubleTicket(troubleTicketRepository.save(ticket));
    }

    public Note addTroubleTicketNote(String id,NoteCreateRequest request) {
        String tenantId = currentTenant.getTenantId();

        TroubleTicketEntity ticket = troubleTicketRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        ticket.addNote(request.text());
        TroubleTicketEntity savedTicket = troubleTicketRepository.save(ticket);
        List<NoteEntity> notes = savedTicket.getNotes();
        return toNote(notes.getLast());
    }

    private TroubleTicket toTroubleTicket(TroubleTicketEntity savedTicket) {
        return new TroubleTicket(
                savedTicket.getId(),
                savedTicket.getExternalId(),
                savedTicket.getServiceId(),
                savedTicket.getDescription(),
                savedTicket.getStatus(),
                mapNotes(savedTicket.getNotes())
        );
    }


    private List<Note> mapNotes(List<NoteEntity> notes) {
        return notes.stream()
                .map(this::toNote)
                .toList();
    }

    private Note toNote(NoteEntity note) {
        return new Note(
                note.getId(),
                note.getText(),
                note.getDate()
        );
    }
}
