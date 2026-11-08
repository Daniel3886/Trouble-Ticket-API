package org.springboot.troubleticketapi.service;

import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
import org.springboot.troubleticketapi.repository.TroubleTicketRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TicketPersistence {
    private final TroubleTicketRepository troubleTicketRepository;

    TicketPersistence(TroubleTicketRepository troubleTicketRepository) {
        this.troubleTicketRepository = troubleTicketRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    TroubleTicketEntity persist(TroubleTicketEntity ticket) {
        return troubleTicketRepository.saveAndFlush(ticket);
    }
}
