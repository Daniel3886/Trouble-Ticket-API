package org.springboot.troubleticketapi.service;

import org.springboot.troubleticketapi.dto.*;
import org.springboot.troubleticketapi.entity.NoteEntity;
import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
import org.springboot.troubleticketapi.exception.ServiceNotFoundException;
import org.springboot.troubleticketapi.exception.TicketNotFoundException;
import org.springboot.troubleticketapi.repository.TroubleTicketRepository;
import org.springboot.troubleticketapi.repository.ServiceRepository;
import org.springboot.troubleticketapi.security.CurrentTenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TroubleTicketService {

    private final TroubleTicketRepository troubleTicketRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentTenant currentTenant;

    public TroubleTicketService(
            TroubleTicketRepository troubleTicketRepository,
            ServiceRepository serviceRepository,
            CurrentTenant currentTenant
    ) {
        this.troubleTicketRepository = troubleTicketRepository;
        this.serviceRepository = serviceRepository;
        this.currentTenant = currentTenant;
    }

    @Transactional
    public CreateResult createTroubleTicket(TroubleTicketCreateRequest request) {
        String tenantId = currentTenant.getTenantId();

        if (request.status() != TroubleTicketCreateStatus.NEW) {
            throw new IllegalArgumentException("New tickets must have status 'new'");
        }

        Optional<TroubleTicketEntity> existing =
                troubleTicketRepository.findByTenantIdAndExternalId(tenantId, request.externalId());
        if (existing.isPresent()) {
            return new CreateResult(toTroubleTicket(existing.get()), false);
        }

        if (!serviceRepository.existsByIdAndTenantIdAndActiveTrue(request.serviceId(), tenantId)) {
            throw new ServiceNotFoundException(request.serviceId());
        }

        TroubleTicketEntity troubleTicket = new TroubleTicketEntity(
                tenantId,
                request.externalId(),
                request.serviceId(),
                request.description(),
                TroubleTicketStatus.NEW
        );

        troubleTicket.addNote(request.note());

        TroubleTicketEntity savedTicket = troubleTicketRepository.save(troubleTicket);
        return new CreateResult(toTroubleTicket(savedTicket), true);
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

    @Transactional(readOnly = true)
    public TroubleTicket getTroubleTicketById(String id) {
        String tenantId = currentTenant.getTenantId();

        TroubleTicketEntity ticket = troubleTicketRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(
                        () -> new TicketNotFoundException("Ticket not found with id: " + id)
                );
        return toTroubleTicket(ticket);
    }

    @Transactional
    public TroubleTicket closeTroubleTicket(
            String id,
            TroubleTicketCloseStatusRequest request
    ) {
        String tenantId = currentTenant.getTenantId();

        if (request.status() != TroubleTicketCloseStatus.CLOSED) {
            throw new IllegalArgumentException("Trouble tickets can only be closed with status 'closed'");
        }

        TroubleTicketEntity ticket = troubleTicketRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Trouble ticket not found: " + id));

        ticket.close();
        return toTroubleTicket(troubleTicketRepository.save(ticket));
    }

    @Transactional
    public Note addTroubleTicketNote(String id, NoteCreateRequest request) {
        String tenantId = currentTenant.getTenantId();

        TroubleTicketEntity ticket = troubleTicketRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        NoteEntity note = ticket.addNote(request.text());
        troubleTicketRepository.save(ticket);

        return toNote(note);
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
