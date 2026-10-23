package org.springboot.troubleticketapi.service;

import jakarta.validation.Valid;
import org.springboot.troubleticketapi.dto.CreateNoteRequest;
import org.springboot.troubleticketapi.dto.CreateTroubleTicketRequest;
import org.springboot.troubleticketapi.dto.NoteResponse;
import org.springboot.troubleticketapi.dto.TroubleTicketResponse;
import org.springboot.troubleticketapi.entity.Note;
import org.springboot.troubleticketapi.entity.TroubleTicket;
import org.springboot.troubleticketapi.entity.TroubleTicketStatus;
import org.springboot.troubleticketapi.repository.TroubleTicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TroubleTicketService {

    private final TroubleTicketRepository troubleTicketRepository;

    public TroubleTicketService(TroubleTicketRepository troubleTicketRepository) {
        this.troubleTicketRepository = troubleTicketRepository;
    }

    public TroubleTicketResponse createTroubleTicket(
            @Valid CreateTroubleTicketRequest request) {

        TroubleTicket troubleTicket = new TroubleTicket();

        troubleTicket.setExternalId(request.externalId());
        troubleTicket.setServiceId(request.serviceId());
        troubleTicket.setDescription(request.description());

        TroubleTicketStatus status =
                TroubleTicketStatus.fromValue(request.status());


        troubleTicket.setStatus(status);
        troubleTicket.addNote(request.note());

        TroubleTicket savedTicket = troubleTicketRepository.save(troubleTicket);

        return new TroubleTicketResponse(
                savedTicket.getId(),
                savedTicket.getExternalId(),
                savedTicket.getServiceId(),
                savedTicket.getDescription(),
                savedTicket.getStatus().getValue(),
                mapNotes(savedTicket.getNotes())
        );
    }


    public List<TroubleTicket> getTroubleAllTickets() {
        return null;
    }

    public TroubleTicket getTroubleTicketById(Long id) {
        return null;
    }

    public TroubleTicket updateNote(@Valid CreateNoteRequest request) {
        return null;
    }

    private List<NoteResponse> mapNotes(List<Note> notes) {
        return notes.stream()
                .map(note -> new NoteResponse(
                   note.getId(),
                   note.getText(),
                   note.getDate()
                ))
                .toList();
    }

}
