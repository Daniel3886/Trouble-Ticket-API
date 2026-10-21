package org.springboot.troubleticketapi.repository;

import org.springboot.troubleticketapi.entity.TroubleTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TroubleTicketRepository extends JpaRepository<TroubleTicket, Long> {
}
