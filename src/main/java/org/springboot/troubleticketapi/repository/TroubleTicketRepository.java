package org.springboot.troubleticketapi.repository;

import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TroubleTicketRepository
        extends JpaRepository<TroubleTicketEntity, String> {}
