package org.springboot.troubleticketapi.service;

import jakarta.validation.Valid;
import org.springboot.troubleticketapi.dto.Note;
import org.springboot.troubleticketapi.dto.NoteCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicket;
import org.springboot.troubleticketapi.dto.TroubleTicketCloseStatusRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateStatus;
import org.springboot.troubleticketapi.dto.TroubleTicketStatus;
import org.springboot.troubleticketapi.dto.TroubleTicketSummary;
import org.springboot.troubleticketapi.entity.NoteEntity;
import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
import org.springboot.troubleticketapi.repository.TroubleTicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TroubleTicketService {

    private final TroubleTicketRepository troubleTicketRepository;

    public TroubleTicketService(TroubleTicketRepository troubleTicketRepository) {
        this.troubleTicketRepository = troubleTicketRepository;
    }

    public TroubleTicket createTroubleTicket(
            @Valid TroubleTicketCreateRequest request
    ) {

        if (request.status() != TroubleTicketCreateStatus.NEW) {
            throw new IllegalArgumentException("New tickets must have status 'new'");
        }

        TroubleTicketEntity troubleTicket = new TroubleTicketEntity();

        troubleTicket.setExternalId(request.externalId());
        troubleTicket.setServiceId(request.serviceId());
        troubleTicket.setDescription(request.description());

            TroubleTicketStatus status =
                TroubleTicketStatus.NEW;


        troubleTicket.setStatus(status);
        troubleTicket.addNote(request.note());

        TroubleTicketEntity savedTicket = troubleTicketRepository.save(troubleTicket);

        return toTroubleTicket(savedTicket);
    }



    public List<TroubleTicketSummary> getTroubleAllTickets() {
            return null;
    }

    public TroubleTicket getTroubleTicketById(String id) {
        return null;
    }

    public TroubleTicket closeTroubleTicket(
            String id,
            @Valid TroubleTicketCloseStatusRequest request) {
        return null;
    }

    public Note addTroubleTicketNote(String id, @Valid NoteCreateRequest request) {
        return null;
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
        return new Note(note.getId(), note.getText(), note.getDate());
    }
}
