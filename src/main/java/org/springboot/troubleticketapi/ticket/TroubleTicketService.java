package org.springboot.troubleticketapi.ticket;

import java.util.List;
import java.util.Optional;
import org.springboot.troubleticketapi.security.CurrentTenant;
import org.springboot.troubleticketapi.servicecatalog.ServiceCatalog;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TroubleTicketService {

    private final TroubleTicketRepository troubleTicketRepository;
    private final ServiceCatalog serviceCatalog;
    private final CurrentTenant currentTenant;

    public TroubleTicketService(
            TroubleTicketRepository troubleTicketRepository,
            ServiceCatalog serviceCatalog,
            CurrentTenant currentTenant) {
        this.troubleTicketRepository = troubleTicketRepository;
        this.serviceCatalog = serviceCatalog;
        this.currentTenant = currentTenant;
    }

    public CreateResult createTroubleTicket(TroubleTicketCreateRequest request) {
        String tenantId = currentTenant.getTenantId();

        if (request.status() != TroubleTicketStatus.NEW) {
            throw new InvalidStatusException("Field 'status' must be 'new'");
        }

        Optional<TroubleTicketEntity> existing =
                troubleTicketRepository.findByTenantIdAndExternalId(tenantId, request.externalId());
        if (existing.isPresent()) {
            return new CreateResult(toTroubleTicket(existing.get()), false);
        }

        serviceCatalog.requireActiveService(request.serviceId(), tenantId);

        TroubleTicketEntity troubleTicket = new TroubleTicketEntity(
                tenantId, request.externalId(), request.serviceId(), request.description(), TroubleTicketStatus.NEW);

        troubleTicket.addNote(request.note());

        try {
            troubleTicketRepository.saveAndFlush(troubleTicket);
            return new CreateResult(toTroubleTicket(troubleTicket), true);
        } catch (DataIntegrityViolationException duplicateExternalId) {
            TroubleTicketEntity concurrentTicket = troubleTicketRepository
                    .findByTenantIdAndExternalId(tenantId, request.externalId())
                    .orElseThrow(() -> duplicateExternalId);
            return new CreateResult(toTroubleTicket(concurrentTicket), false);
        }
    }

    public List<TroubleTicketSummary> getTroubleAllTickets() {
        String tenantId = currentTenant.getTenantId();

        return troubleTicketRepository.findAllByTenantId(tenantId).stream()
                .map(ticket -> new TroubleTicketSummary(
                        ticket.getExternalId(), ticket.getServiceId(), ticket.getDescription(), ticket.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TroubleTicket getTroubleTicketById(String id) {
        String tenantId = currentTenant.getTenantId();

        TroubleTicketEntity ticket = troubleTicketRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
        return toTroubleTicket(ticket);
    }

    @Transactional
    public TroubleTicket closeTroubleTicket(String id, TroubleTicketCloseStatusRequest request) {
        String tenantId = currentTenant.getTenantId();

        if (request.status() != TroubleTicketStatus.CLOSED) {
            throw new InvalidStatusException("Field 'status' must be 'closed'");
        }

        TroubleTicketEntity ticket = troubleTicketRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TicketNotFoundException("Trouble ticket not found: " + id));

        ticket.close();
        return toTroubleTicket(troubleTicketRepository.save(ticket));
    }

    @Transactional
    public Note addTroubleTicketNote(String id, NoteCreateRequest request) {
        String tenantId = currentTenant.getTenantId();

        TroubleTicketEntity ticket = troubleTicketRepository
                .findByIdAndTenantId(id, tenantId)
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
                mapNotes(savedTicket.getNotes()));
    }

    private List<Note> mapNotes(List<NoteEntity> notes) {
        return notes.stream().map(this::toNote).toList();
    }

    private Note toNote(NoteEntity note) {
        return new Note(note.getId(), note.getText(), note.getDate());
    }
}
