package org.springboot.troubleticketapi.controller;

import jakarta.validation.Valid;
import org.springboot.troubleticketapi.dto.Note;
import org.springboot.troubleticketapi.dto.NoteCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicket;
import org.springboot.troubleticketapi.dto.TroubleTicketCloseStatusRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketSummary;
import org.springboot.troubleticketapi.service.TroubleTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;


@RequestMapping("/api/v1/troubleTicket")
@RestController
public class TroubleTicketController {

    private final TroubleTicketService troubleTicketService;

    public TroubleTicketController(TroubleTicketService troubleTicketService) {
        this.troubleTicketService = troubleTicketService;
    }

    @PostMapping
    public ResponseEntity<TroubleTicket> createTroubleTicket(
            @Valid @RequestBody TroubleTicketCreateRequest request) {

        TroubleTicket response =
                troubleTicketService.createTroubleTicket(request);

        URI location = URI.create(
                "/api/v1/troubleTicket/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<TroubleTicketSummary> getTroubleAllTickets(){
        return troubleTicketService.getTroubleAllTickets();
    }

    @GetMapping("/{id}")
    public TroubleTicket getTroubleTicketById(@PathVariable String id){
        return troubleTicketService.getTroubleTicketById(id);
    }

    @PatchMapping("/{id}")
    public TroubleTicket closeTroubleTicket(
            @PathVariable String id,
            @Valid @RequestBody TroubleTicketCloseStatusRequest request) {
        return troubleTicketService.closeTroubleTicket(id, request);
    }

    @PostMapping("/{id}/note")
    public ResponseEntity<Note> addTroubleTicketNote(
            @PathVariable String id,
            @Valid @RequestBody NoteCreateRequest request) {
        return ResponseEntity.status(201).body(troubleTicketService.addTroubleTicketNote(id, request));
    }

}
