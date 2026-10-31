package org.springboot.troubleticketapi.exception;

public class MissingTenantException extends RuntimeException {
    public MissingTenantException() {
        super("Token doesn't contain tenant_id");
    }
}
