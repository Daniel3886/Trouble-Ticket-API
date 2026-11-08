package org.springboot.troubleticketapi.security;

import org.springboot.troubleticketapi.exception.MissingTenantException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenant {

    public String getTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException("No JWT authentication is avaliable");
        }

        String tenantId = jwtAuthentication.getToken().getClaimAsString("tenant_id");

        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantException();
        }

        return tenantId;
    }
}
