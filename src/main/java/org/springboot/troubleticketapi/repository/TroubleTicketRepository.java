package org.springboot.troubleticketapi.repository;

import java.util.List;
import java.util.Optional;
import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TroubleTicketRepository extends JpaRepository<TroubleTicketEntity, String> {

    Optional<TroubleTicketEntity> findByTenantIdAndExternalId(String tenantId, String externalId);

    List<TroubleTicketEntity> findAllByTenantId(String tenantId);

    Optional<TroubleTicketEntity> findByIdAndTenantId(String id, String tenantId);
}
