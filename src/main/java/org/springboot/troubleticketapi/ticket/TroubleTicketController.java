package org.springboot.troubleticketapi.ticket;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/troubleTicket")
@RestController
class TroubleTicketController {

    private final TroubleTicketService troubleTicketService;

    public TroubleTicketController(TroubleTicketService troubleTicketService) {
        this.troubleTicketService = troubleTicketService;
    }

    @PostMapping
    public ResponseEntity<TroubleTicket> createTroubleTicket(@Valid @RequestBody TroubleTicketCreateRequest request) {

        CreateResult result = troubleTicketService.createTroubleTicket(request);
        TroubleTicket ticket = result.ticket();

        URI location = URI.create("/api/v1/troubleTicket/" + ticket.id());

        if (!result.created()) {
            return ResponseEntity.ok().location(location).body(ticket);
        }

        return ResponseEntity.created(location).body(ticket);
    }

    @GetMapping
    public List<TroubleTicketSummary> getAllTroubleTickets() {
        return troubleTicketService.getAllTroubleTickets();
    }

    @GetMapping("/{id}")
    public TroubleTicket getTroubleTicketById(@PathVariable String id) {
        return troubleTicketService.getTroubleTicketById(id);
    }

    @PatchMapping("/{id}")
    public TroubleTicket closeTroubleTicket(
            @PathVariable String id, @Valid @RequestBody TroubleTicketCloseStatusRequest request) {
        return troubleTicketService.closeTroubleTicket(id, request);
    }

    @PostMapping("/{id}/note")
    public ResponseEntity<Note> addTroubleTicketNote(
            @PathVariable String id, @Valid @RequestBody NoteCreateRequest request) {
        return ResponseEntity.status(201).body(troubleTicketService.addTroubleTicketNote(id, request));
    }
}
