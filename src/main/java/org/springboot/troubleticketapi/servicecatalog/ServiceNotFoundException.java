package org.springboot.troubleticketapi.servicecatalog;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(Long serviceId) {
        super("Service not found: " + serviceId);
    }
}
