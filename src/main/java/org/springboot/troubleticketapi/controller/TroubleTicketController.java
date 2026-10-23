package org.springboot.troubleticketapi.controller;

import jakarta.validation.Valid;
import org.springboot.troubleticketapi.dto.CreateNoteRequest;
import org.springboot.troubleticketapi.dto.CreateTroubleTicketRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketResponse;
import org.springboot.troubleticketapi.entity.TroubleTicket;
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
    public ResponseEntity<TroubleTicketResponse> createTroubleTicket(
            @Valid @RequestBody CreateTroubleTicketRequest request) {

        TroubleTicketResponse response =
                troubleTicketService.createTroubleTicket(request);

        URI location = URI.create(
                "/api/v1/troubleTicket/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<TroubleTicket> getTroubleAllTickets(){
        return troubleTicketService.getTroubleAllTickets();
    }

    @GetMapping
    public TroubleTicket getTroubleTicketById(@RequestParam Long id){
        return troubleTicketService.getTroubleTicketById(id);
    }

    @PatchMapping
    public TroubleTicket updateNote(@Valid @RequestBody CreateNoteRequest request){
        return troubleTicketService.updateNote(request);
    }

}
