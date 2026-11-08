package org.springboot.troubleticketapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springboot.troubleticketapi.dto.Error;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class JsonSecurityErrorHandler {
    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new Error(code, message, (String)
                request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }
}
