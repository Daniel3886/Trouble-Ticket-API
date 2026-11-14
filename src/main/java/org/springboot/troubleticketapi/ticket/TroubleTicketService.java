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

        TroubleTicketEntity troubleTicket =
                new TroubleTicketEntity(tenantId, request.externalId(), request.serviceId(), request.description());

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

    @Transactional(readOnly = true)
    public List<TroubleTicketSummary> getAllTroubleTickets() {
        String tenantId = currentTenant.getTenantId();

        return troubleTicketRepository.findAllByTenantId(tenantId).stream()
                .map(ticket -> new TroubleTicketSummary(
                        ticket.getExternalId(), ticket.getServiceId(), ticket.getDescription(), ticket.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TroubleTicket getTroubleTicketById(String id) {
        return toTroubleTicket(findTicket(id));
    }

    @Transactional
    public TroubleTicket closeTroubleTicket(String id, TroubleTicketCloseStatusRequest request) {
        if (request.status() != TroubleTicketStatus.CLOSED) {
            throw new InvalidStatusException("Field 'status' must be 'closed'");
        }

        TroubleTicketEntity ticket = findTicket(id);
        ticket.close();
        return toTroubleTicket(troubleTicketRepository.save(ticket));
    }

    @Transactional
    public Note addTroubleTicketNote(String id, NoteCreateRequest request) {
        TroubleTicketEntity ticket = findTicket(id);
        NoteEntity note = ticket.addNote(request.text());
        troubleTicketRepository.save(ticket);

        return toNote(note);
    }

    private TroubleTicketEntity findTicket(String id) {
        return troubleTicketRepository
                .findByIdAndTenantId(id, currentTenant.getTenantId())
                .orElseThrow(() -> new TicketNotFoundException("Trouble ticket not found: " + id));
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
