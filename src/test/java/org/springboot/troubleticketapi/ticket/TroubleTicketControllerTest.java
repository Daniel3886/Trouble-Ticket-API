package org.springboot.troubleticketapi.ticket;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springboot.troubleticketapi.security.SecurityTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(TroubleTicketController.class)
@Import(SecurityTestConfig.class)
class TroubleTicketControllerTest {

    private static final String URL = "/api/v1/troubleTicket";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TroubleTicketService troubleTicketService;

    @Test
    void createReturns201WithLocationForNewTicket() throws Exception {
        // given
        given(troubleTicketService.createTroubleTicket(any()))
                .willReturn(new CreateResult(TicketFixtures.ticket(TroubleTicketStatus.NEW), true));

        // when / then
        mockMvc.perform(post(URL)
                        .with(tenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TicketFixtures.createJson()))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", URL + "/" + TicketFixtures.TICKET_ID),
                        jsonPath("$.id").value(TicketFixtures.TICKET_ID),
                        jsonPath("$.externalId").value(TicketFixtures.EXTERNAL_ID),
                        jsonPath("$.status").value("new"),
                        jsonPath("$.notes[0].text").value("Initial note"));
    }

    @Test
    void createReturns200ForExistingExternalId() throws Exception {
        // given
        given(troubleTicketService.createTroubleTicket(any()))
                .willReturn(new CreateResult(TicketFixtures.ticket(TroubleTicketStatus.NEW), false));

        // when / then
        mockMvc.perform(post(URL)
                        .with(tenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TicketFixtures.createJson()))
                .andExpectAll(
                        status().isOk(),
                        header().string("Location", URL + "/" + TicketFixtures.TICKET_ID),
                        jsonPath("$.id").value(TicketFixtures.TICKET_ID));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCreateBodies")
    void createRejectsInvalidBodyWith400(String scenario, String body, String expectedMessagePart) throws Exception {
        mockMvc.perform(post(URL)
                        .with(tenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_ERROR"),
                        jsonPath("$.message", containsString(expectedMessagePart)),
                        jsonPath("$.requestId", notNullValue()));

        verifyNoInteractions(troubleTicketService);
    }

    static Stream<Arguments> invalidCreateBodies() {
        return Stream.of(
                arguments("missing externalId", """
                        {"serviceId":1,"description":"d","status":"new","note":"n"}""", "'externalId'"),
                arguments("blank description", """
                        {"externalId":"e","serviceId":1,"description":" ","status":"new","note":"n"}""", "'description'"),
                arguments("negative serviceId", """
                        {"externalId":"e","serviceId":-1,"description":"d","status":"new","note":"n"}""", "'serviceId'"),
                arguments("missing status", """
                        {"externalId":"e","serviceId":1,"description":"d","note":"n"}""", "'status'"),
                arguments("unknown status value", """
                        {"externalId":"e","serviceId":1,"description":"d","status":"foo","note":"n"}""", "Field 'status' has an invalid value"),
                arguments("unknown field", """
                        {"externalId":"e","serviceId":1,"description":"d","status":"new","note":"n","priority":1}""", "Field 'priority' is not allowed"),
                arguments("malformed JSON", "{", "Malformed or invalid request body"));
    }

    @Test
    void createReturns400WhenServiceRejectsStatus() throws Exception {
        // given
        given(troubleTicketService.createTroubleTicket(any()))
                .willThrow(new InvalidStatusException("Field 'status' must be 'new'"));

        // when / then
        mockMvc.perform(post(URL)
                        .with(tenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TicketFixtures.createJson().replace("\"new\"", "\"resolved\"")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_ERROR"),
                        jsonPath("$.message").value("Field 'status' must be 'new'"));
    }

    @Test
    void getReturns404WithErrorBodyForUnknownTicket() throws Exception {
        // given
        given(troubleTicketService.getTroubleTicketById("missing"))
                .willThrow(new TicketNotFoundException("Ticket not found with id: missing"));

        // when / then
        mockMvc.perform(get(URL + "/missing").with(tenantA()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.code").value("TROUBLE_TICKET_NOT_FOUND"),
                        jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void closeReturns200WithClosedTicket() throws Exception {
        // given
        given(troubleTicketService.closeTroubleTicket(eq(TicketFixtures.TICKET_ID), any()))
                .willReturn(TicketFixtures.ticket(TroubleTicketStatus.CLOSED));

        // when / then
        mockMvc.perform(patch(URL + "/" + TicketFixtures.TICKET_ID)
                        .with(tenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"closed\"}"))
                .andExpectAll(status().isOk(), jsonPath("$.status").value("closed"));
    }

    @Test
    void requestWithoutTokenReturns401InErrorFormat() throws Exception {
        mockMvc.perform(get(URL))
                .andExpectAll(
                        status().isUnauthorized(),
                        header().exists("X-Request-ID"),
                        jsonPath("$.code").value("UNAUTHORIZED"),
                        jsonPath("$.requestId", notNullValue()));

        verifyNoInteractions(troubleTicketService);
    }

    @Test
    void unexpectedExceptionReturnsGeneric500WithoutInternalDetails() throws Exception {
        // given: an exception whose message must never reach the client
        given(troubleTicketService.getTroubleTicketById("any"))
                .willThrow(new IllegalArgumentException("SQL: select * from trouble_tickets where secret=42"));

        // when / then
        mockMvc.perform(get(URL + "/any").with(tenantA()))
                .andExpectAll(
                        status().isInternalServerError(),
                        jsonPath("$.code").value("INTERNAL_ERROR"),
                        jsonPath("$.message", not(containsString("secret"))),
                        jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void concurrentModificationReturns409() throws Exception {
        // given
        given(troubleTicketService.closeTroubleTicket(eq(TicketFixtures.TICKET_ID), any()))
                .willThrow(new OptimisticLockingFailureException("Row was updated by another transaction"));

        // when / then
        mockMvc.perform(patch(URL + "/" + TicketFixtures.TICKET_ID)
                        .with(tenantA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"closed\"}"))
                .andExpectAll(status().isConflict(), jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void springServerErrorIsNotReportedAsClientError() throws Exception {
        // given: a Spring web exception that carries a 5xx status
        given(troubleTicketService.getTroubleTicketById("any"))
                .willThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "internal detail"));

        // when / then
        mockMvc.perform(get(URL + "/any").with(tenantA()))
                .andExpectAll(
                        status().isInternalServerError(),
                        jsonPath("$.code").value("INTERNAL_ERROR"),
                        jsonPath("$.message", not(containsString("internal detail"))));
    }

    @Test
    void unsupportedHttpMethodReturns405InErrorFormat() throws Exception {
        mockMvc.perform(delete(URL + "/" + TicketFixtures.TICKET_ID).with(tenantA()))
                .andExpectAll(
                        status().isMethodNotAllowed(),
                        jsonPath("$.code").value("METHOD_NOT_ALLOWED"),
                        jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void unknownPathReturns404InErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/unknown").with(tenantA()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.code").value("NOT_FOUND"),
                        jsonPath("$.requestId", notNullValue()));
    }

    private static RequestPostProcessor tenantA() {
        return jwt().jwt(token -> token.claim("tenant_id", "tenant-a"));
    }
}
