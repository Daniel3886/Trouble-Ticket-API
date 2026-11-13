package org.springboot.troubleticketapi.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({SecurityConfig.class, JsonSecurityErrorHandler.class})
public class SecurityTestConfig {}
