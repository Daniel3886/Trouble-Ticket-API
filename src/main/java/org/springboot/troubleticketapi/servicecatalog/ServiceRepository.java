package org.springboot.troubleticketapi.servicecatalog;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    boolean existsByIdAndTenantIdAndActiveTrue(Long serviceId, String tenantId);
}
