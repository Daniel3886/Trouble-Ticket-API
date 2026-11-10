package org.springboot.troubleticketapi.ticket;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class TicketPersistence {
    private final TroubleTicketRepository troubleTicketRepository;

    TicketPersistence(TroubleTicketRepository troubleTicketRepository) {
        this.troubleTicketRepository = troubleTicketRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    TroubleTicketEntity persist(TroubleTicketEntity ticket) {
        return troubleTicketRepository.saveAndFlush(ticket);
    }
}
