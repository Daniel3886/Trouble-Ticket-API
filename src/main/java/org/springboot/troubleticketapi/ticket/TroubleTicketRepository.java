package org.springboot.troubleticketapi.ticket;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface TroubleTicketRepository extends JpaRepository<TroubleTicketEntity, String> {

    Optional<TroubleTicketEntity> findByTenantIdAndExternalId(String tenantId, String externalId);

    List<TroubleTicketEntity> findAllByTenantId(String tenantId);

    Optional<TroubleTicketEntity> findByIdAndTenantId(String id, String tenantId);
}
