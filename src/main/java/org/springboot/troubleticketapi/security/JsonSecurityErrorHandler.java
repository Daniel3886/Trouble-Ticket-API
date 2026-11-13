package org.springboot.troubleticketapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springboot.troubleticketapi.common.Error;
import org.springboot.troubleticketapi.common.ErrorCode;
import org.springboot.troubleticketapi.common.RequestIdFilter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
class JsonSecurityErrorHandler {
    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new Error(code.name(), code.defaultMessage(), (String)
                request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }
}
