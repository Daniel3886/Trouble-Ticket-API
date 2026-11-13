package org.springboot.troubleticketapi.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springboot.troubleticketapi.security.CurrentTenant;
import org.springboot.troubleticketapi.servicecatalog.ServiceCatalog;
import org.springboot.troubleticketapi.servicecatalog.ServiceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TroubleTicketServiceTest {

    @Mock
    TroubleTicketRepository troubleTicketRepository;

    @Mock
    ServiceCatalog serviceCatalog;

    @Mock
    CurrentTenant tenant;

    @InjectMocks
    TroubleTicketService troubleTicketService;

    private static final String TICKET_ID = "8a56c5af-aeb7-4b6b-b493-25f7e74207f0";
    private static final String TENANT_ID = "tenant-1";

    @BeforeEach
    void setUp() {
        when(tenant.getTenantId()).thenReturn(TENANT_ID);
        lenient()
                .when(troubleTicketRepository.saveAndFlush(any(TroubleTicketEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateTicket() {
        // given
        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketStatus.NEW, "Initial note");

        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.empty());

        // when
        var response = troubleTicketService.createTroubleTicket(request);

        // then
        assertEquals("OK-123456", response.ticket().externalId());
        assertEquals(987654321L, response.ticket().serviceId());
        assertEquals("No data connection", response.ticket().description());
        assertEquals("new", response.ticket().status().getValue());
        assertEquals("Initial note", response.ticket().notes().getFirst().text());
        assertTrue(response.created());

        verify(troubleTicketRepository).saveAndFlush(any(TroubleTicketEntity.class));
    }

    @Test
    void shouldRejectCreateWhenStatusIsNotNew() {
        // given
        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketStatus.RESOLVED, "Initial note");

        // when
        InvalidStatusException exception =
                assertThrows(InvalidStatusException.class, () -> troubleTicketService.createTroubleTicket(request));

        // then
        assertEquals("Field 'status' must be 'new'", exception.getMessage());
        verifyNoInteractions(troubleTicketRepository, serviceCatalog);
    }

    @Test
    void shouldReturnExistingTicketWithoutSavingWhenExternalIdExists() {
        // given
        TroubleTicketEntity existing = new TroubleTicketEntity(
                TENANT_ID, "OK-123456", 987654321L, "No data connection", TroubleTicketStatus.IN_PROGRESS);

        existing.addNote("Initial note");

        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.of(existing));

        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "A different description", TroubleTicketStatus.NEW, "A different note");

        // when
        CreateResult result = troubleTicketService.createTroubleTicket(request);

        // then
        assertFalse(result.created());
        assertEquals(existing.getId(), result.ticket().id());
        assertEquals(TroubleTicketStatus.IN_PROGRESS, result.ticket().status());
        assertEquals("No data connection", result.ticket().description());
        assertEquals(1, result.ticket().notes().size());

        verify(troubleTicketRepository, never()).save(any());
    }

    @Test
    void shouldReturnConcurrentlyCreatedTicketWhenInsertHitsUniqueConstraint() {
        // given: the first lookup finds nothing, but another request inserts the same externalId before us
        TroubleTicketEntity winner = new TroubleTicketEntity(
                TENANT_ID, "OK-123456", 987654321L, "Created by the other request", TroubleTicketStatus.NEW);
        winner.addNote("Other note");

        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.empty(), Optional.of(winner));

        when(troubleTicketRepository.saveAndFlush(any(TroubleTicketEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique_tenant_external_id"));

        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketStatus.NEW, "Initial note");

        // when
        CreateResult result = troubleTicketService.createTroubleTicket(request);

        // then
        assertFalse(result.created());
        assertEquals(winner.getId(), result.ticket().id());
        assertEquals("Created by the other request", result.ticket().description());
        verify(troubleTicketRepository, times(2)).findByTenantIdAndExternalId(TENANT_ID, "OK-123456");
    }

    @Test
    void shouldAssignCurrentTenantToNewTicket() {
        // given
        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.empty());
        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketStatus.NEW, "Initial note");

        // when
        troubleTicketService.createTroubleTicket(request);

        // then
        ArgumentCaptor<TroubleTicketEntity> captor = ArgumentCaptor.forClass(TroubleTicketEntity.class);
        verify(troubleTicketRepository).saveAndFlush(captor.capture());

        TroubleTicketEntity saved = captor.getValue();
        assertEquals(TENANT_ID, saved.getTenantId());
        assertEquals("OK-123456", saved.getExternalId());
        assertEquals(TroubleTicketStatus.NEW, saved.getStatus());
        assertEquals(1, saved.getNotes().size());
        assertEquals("Initial note", saved.getNotes().getFirst().getText());
    }

    @Test
    void shouldThrowServiceNotFoundExceptionWhenServiceDoesNotExist() {
        // given
        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.empty());
        doThrow(new ServiceNotFoundException(987654321L))
                .when(serviceCatalog)
                .requireActiveService(987654321L, TENANT_ID);

        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketStatus.NEW, "Initial note");

        // when / then
        assertThrows(ServiceNotFoundException.class, () -> troubleTicketService.createTroubleTicket(request));

        verify(serviceCatalog).requireActiveService(987654321L, TENANT_ID);
        verify(troubleTicketRepository, never()).save(any());
    }

    @Test
    void shouldReturnTwoTroubleTicketSummaries() {
        // given
        TroubleTicketEntity ticket1 = new TroubleTicketEntity(
                TENANT_ID,
                "8a56c5af-aeb7-4b6b-b493-25f7e74207f0",
                322,
                "new summary for the ticket",
                TroubleTicketStatus.NEW);

        TroubleTicketEntity ticket2 = new TroubleTicketEntity();
        ticket2.setExternalId("52dc8009-5bb6-4637-89b5-caf213238313");
        ticket2.setServiceId(828);
        ticket2.setDescription("acknowledged summary for the ticket");
        ticket2.setStatus(TroubleTicketStatus.ACKNOWLEDGED);

        when(troubleTicketRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(ticket1, ticket2));

        // when
        List<TroubleTicketSummary> result = troubleTicketService.getTroubleAllTickets();

        // then
        assertEquals(2, result.size());

        assertEquals("8a56c5af-aeb7-4b6b-b493-25f7e74207f0", result.getFirst().externalId());
        assertEquals(322, result.getFirst().serviceId());
        assertEquals("new summary for the ticket", result.getFirst().description());
        assertEquals(TroubleTicketStatus.NEW, result.getFirst().status());

        assertEquals("52dc8009-5bb6-4637-89b5-caf213238313", result.get(1).externalId());
        assertEquals(828, result.get(1).serviceId());
        assertEquals("acknowledged summary for the ticket", result.get(1).description());
        assertEquals(TroubleTicketStatus.ACKNOWLEDGED, result.get(1).status());
        verify(troubleTicketRepository).findAllByTenantId(TENANT_ID);
    }

    @Test
    void shouldReturnEmptyList() {
        // given
        when(troubleTicketRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        // when
        List<TroubleTicketSummary> result = troubleTicketService.getTroubleAllTickets();

        // then
        assertEquals(0, result.size());
    }

    @Test
    void shouldReturnTroubleTicketWithMappedFieldsAndNotes() {
        // given
        TroubleTicketEntity ticket = new TroubleTicketEntity(
                TENANT_ID,
                "f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec",
                322,
                "new summary for the ticket",
                TroubleTicketStatus.NEW);
        ticket.addNote("first note");

        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.of(ticket));

        // when
        TroubleTicket result = troubleTicketService.getTroubleTicketById(TICKET_ID);

        // then
        assertEquals("f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec", result.externalId());
        assertEquals(322, result.serviceId());
        assertEquals("new summary for the ticket", result.description());
        assertEquals(TroubleTicketStatus.NEW, result.status());
        assertEquals(1, result.notes().size());
        assertEquals("first note", result.notes().getFirst().text());
        verify(troubleTicketRepository).findByIdAndTenantId(TICKET_ID, TENANT_ID);
    }

    @Test
    void shouldThrowTicketNotFoundExceptionWhenTicketNotFoundById() {
        // given
        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.empty());

        // when
        TicketNotFoundException exception =
                assertThrows(TicketNotFoundException.class, () -> troubleTicketService.getTroubleTicketById(TICKET_ID));

        // then
        assertTrue(exception.getMessage().contains(TICKET_ID));
        verify(troubleTicketRepository, never()).save(any(TroubleTicketEntity.class));
    }

    @Test
    void shouldCloseTroubleTicket() {
        // given
        TroubleTicketEntity ticket = new TroubleTicketEntity(
                TENANT_ID,
                "f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec",
                322,
                "new summary for the ticket",
                TroubleTicketStatus.IN_PROGRESS);

        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.of(ticket));
        when(troubleTicketRepository.save(any(TroubleTicketEntity.class))).thenReturn(ticket);

        TroubleTicketCloseStatusRequest request = new TroubleTicketCloseStatusRequest(TroubleTicketStatus.CLOSED);

        // when
        TroubleTicket result = troubleTicketService.closeTroubleTicket(TICKET_ID, request);

        // then
        assertEquals(TroubleTicketStatus.CLOSED, result.status());
        assertEquals("f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec", result.externalId());
        assertEquals(322, result.serviceId());
        assertEquals("new summary for the ticket", result.description());
        verify(troubleTicketRepository).save(ticket);
    }

    @Test
    void shouldRejectCloseWhenStatusIsNotClosed() {
        // given
        TroubleTicketCloseStatusRequest request = new TroubleTicketCloseStatusRequest(TroubleTicketStatus.RESOLVED);

        // when
        InvalidStatusException exception = assertThrows(
                InvalidStatusException.class, () -> troubleTicketService.closeTroubleTicket(TICKET_ID, request));

        // then
        assertEquals("Field 'status' must be 'closed'", exception.getMessage());
        verifyNoInteractions(troubleTicketRepository);
    }

    @Test
    void shouldThrowTicketNotFoundWhenClosingMissingTicket() {
        // given
        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.empty());

        TroubleTicketCloseStatusRequest request = new TroubleTicketCloseStatusRequest(TroubleTicketStatus.CLOSED);

        // when / then
        assertThrows(TicketNotFoundException.class, () -> troubleTicketService.closeTroubleTicket(TICKET_ID, request));

        verify(troubleTicketRepository, never()).save(any());
    }

    @Test
    void shouldReturnAddedNoteWhenAddingNoteToTicket() {
        // given
        TroubleTicketEntity ticket = new TroubleTicketEntity(
                TENANT_ID,
                "f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec",
                322,
                "new summary for the ticket",
                TroubleTicketStatus.NEW);
        ticket.addNote("first note");
        String firstNoteId = ticket.getNotes().getFirst().getId();

        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.of(ticket));

        // when
        Note result = troubleTicketService.addTroubleTicketNote(TICKET_ID, new NoteCreateRequest("second Note"));

        // then
        assertEquals("second Note", result.text());
        assertNotNull(result.id());
        assertNotEquals(firstNoteId, result.id());
        assertNotNull(result.date());

        assertEquals(2, ticket.getNotes().size());
        assertTrue(ticket.getNotes().stream().anyMatch(note -> note.getId().equals(result.id())));

        verify(troubleTicketRepository).save(ticket);
    }

    @Test
    void shouldThrowTicketNotFoundExceptionWhenAddingNoteToTroubleTicket() {
        // given
        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.empty());

        NoteCreateRequest request = new NoteCreateRequest("new note for the ticket");

        // when / then
        assertThrows(
                TicketNotFoundException.class, () -> troubleTicketService.addTroubleTicketNote(TICKET_ID, request));

        verify(troubleTicketRepository, never()).save(any());
    }
}
