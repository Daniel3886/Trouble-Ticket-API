package org.springboot.troubleticketapi.repository;

import org.springboot.troubleticketapi.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    boolean existsByIdAndTenantIdAndActiveTrue(Long serviceId, String tenantId);
}
