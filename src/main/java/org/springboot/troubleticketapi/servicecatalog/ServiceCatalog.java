package org.springboot.troubleticketapi.servicecatalog;

import org.springframework.stereotype.Component;

/**
 * The only public entry point of the service catalog. Other features ask it questions; the entity and repository
 * behind it stay package-private.
 */
@Component
public class ServiceCatalog {

    private final ServiceRepository serviceRepository;

    ServiceCatalog(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /** @throws ServiceNotFoundException when the service does not exist, is inactive or belongs to another tenant */
    public void requireActiveService(Long serviceId, String tenantId) {
        if (!serviceRepository.existsByIdAndTenantIdAndActiveTrue(serviceId, tenantId)) {
            throw new ServiceNotFoundException(serviceId);
        }
    }
}
