package org.springboot.troubleticketapi.exception;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(Long serviceId) {
        super("Service not found: " + serviceId);
    }
}
