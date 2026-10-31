package org.springboot.troubleticketapi.exception;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(long serviceId) {
        super("Service not found: " + serviceId);
    }
}
