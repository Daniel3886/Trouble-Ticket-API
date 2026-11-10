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

@ExtendWith(MockitoExtension.class)
class TroubleTicketServiceTest {

    @Mock
    TroubleTicketRepository troubleTicketRepository;

    @Mock
    ServiceCatalog serviceCatalog;

    @Mock
    TicketPersistence ticketPersistence;

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
                .when(ticketPersistence.persist(any(TroubleTicketEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateTicket() {
        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketCreateStatus.NEW, "Initial note");

        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.empty());

        var response = troubleTicketService.createTroubleTicket(request);

        assertEquals("OK-123456", response.ticket().externalId());
        assertEquals(987654321L, response.ticket().serviceId());
        assertEquals("No data connection", response.ticket().description());
        assertEquals("new", response.ticket().status().getValue());
        assertEquals("Initial note", response.ticket().notes().getFirst().text());
        assertTrue(response.created());

        verify(ticketPersistence).persist(any(TroubleTicketEntity.class));
    }

    @Test
    void shouldThrowIllegalArgumentIfStatusIsNull() {
        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "49347def-54b1-44b0-890c-0187018f8273", 987654321L, "No data connection", null, "Initial note");

        assertThrows(IllegalArgumentException.class, () -> troubleTicketService.createTroubleTicket(request));

        verify(troubleTicketRepository, never()).save(any(TroubleTicketEntity.class));
    }

    @Test
    void shouldReturnExistingTicketWithoutSavingWhenExternalIdExists() {
        TroubleTicketEntity existing = new TroubleTicketEntity(
                TENANT_ID, "OK-123456", 987654321L, "No data connection", TroubleTicketStatus.IN_PROGRESS);

        existing.addNote("Initial note");

        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.of(existing));

        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "A different description", TroubleTicketCreateStatus.NEW, "A different note");

        CreateResult result = troubleTicketService.createTroubleTicket(request);

        assertFalse(result.created());
        assertEquals(existing.getId(), result.ticket().id());
        assertEquals(TroubleTicketStatus.IN_PROGRESS, result.ticket().status());
        assertEquals("No data connection", result.ticket().description());
        assertEquals(1, result.ticket().notes().size());

        verify(troubleTicketRepository, never()).save(any());
    }

    @Test
    void shouldAssignCurrentTenantToNewTicket() {
        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.empty());
        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketCreateStatus.NEW, "Initial note");

        troubleTicketService.createTroubleTicket(request);

        ArgumentCaptor<TroubleTicketEntity> captor = ArgumentCaptor.forClass(TroubleTicketEntity.class);
        verify(ticketPersistence).persist(captor.capture());

        TroubleTicketEntity saved = captor.getValue();
        assertEquals(TENANT_ID, saved.getTenantId());
        assertEquals("OK-123456", saved.getExternalId());
        assertEquals(TroubleTicketStatus.NEW, saved.getStatus());
        assertEquals(1, saved.getNotes().size());
        assertEquals("Initial note", saved.getNotes().getFirst().getText());
    }

    @Test
    void shouldThrowServiceNotFoundExceptionWhenServiceDoesNotExist() {
        when(troubleTicketRepository.findByTenantIdAndExternalId(TENANT_ID, "OK-123456"))
                .thenReturn(Optional.empty());
        doThrow(new ServiceNotFoundException(987654321L))
                .when(serviceCatalog)
                .requireActiveService(987654321L, TENANT_ID);

        TroubleTicketCreateRequest request = new TroubleTicketCreateRequest(
                "OK-123456", 987654321L, "No data connection", TroubleTicketCreateStatus.NEW, "Initial note");

        assertThrows(ServiceNotFoundException.class, () -> troubleTicketService.createTroubleTicket(request));

        verify(serviceCatalog).requireActiveService(987654321L, TENANT_ID);
        verify(troubleTicketRepository, never()).save(any());
    }

    @Test
    void shouldReturnTwoTroubleTicketSummaries() {

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

        List<TroubleTicketSummary> result = troubleTicketService.getTroubleAllTickets();

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
        when(troubleTicketRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        List<TroubleTicketSummary> result = troubleTicketService.getTroubleAllTickets();

        assertEquals(0, result.size());
    }

    @Test
    void shouldReturnTroubleTicketWithMappedFieldsAndNotes() {
        TroubleTicketEntity ticket = new TroubleTicketEntity(
                TENANT_ID,
                "f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec",
                322,
                "new summary for the ticket",
                TroubleTicketStatus.NEW);
        ticket.addNote("first note");

        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.of(ticket));

        TroubleTicket result = troubleTicketService.getTroubleTicketById(TICKET_ID);

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
        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.empty());

        TicketNotFoundException exception =
                assertThrows(TicketNotFoundException.class, () -> troubleTicketService.getTroubleTicketById(TICKET_ID));

        assertTrue(exception.getMessage().contains(TICKET_ID));
        verify(troubleTicketRepository, never()).save(any(TroubleTicketEntity.class));
    }

    @Test
    void shouldCloseTroubleTicket() {
        TroubleTicketEntity ticket = new TroubleTicketEntity(
                TENANT_ID,
                "f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec",
                322,
                "new summary for the ticket",
                TroubleTicketStatus.IN_PROGRESS);

        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.of(ticket));
        when(troubleTicketRepository.save(any(TroubleTicketEntity.class))).thenReturn(ticket);

        TroubleTicketCloseStatusRequest request = new TroubleTicketCloseStatusRequest(TroubleTicketCloseStatus.CLOSED);

        TroubleTicket result = troubleTicketService.closeTroubleTicket(TICKET_ID, request);

        assertEquals(TroubleTicketStatus.CLOSED, result.status());
        assertEquals("f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec", result.externalId());
        assertEquals(322, result.serviceId());
        assertEquals("new summary for the ticket", result.description());
        verify(troubleTicketRepository).save(ticket);
    }

    @Test
    void shouldThrowIllegalArgumentWhenCloseStatusIsNull() {
        TroubleTicketCloseStatusRequest request = new TroubleTicketCloseStatusRequest(null);

        assertThrows(IllegalArgumentException.class, () -> troubleTicketService.closeTroubleTicket(TICKET_ID, request));

        verify(troubleTicketRepository, never()).findByIdAndTenantId(any(), any());
        verify(troubleTicketRepository, never()).save(any());
    }

    @Test
    void shouldThrowTicketNotFoundWhenClosingMissingTicket() {
        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.empty());

        TroubleTicketCloseStatusRequest request = new TroubleTicketCloseStatusRequest(TroubleTicketCloseStatus.CLOSED);

        assertThrows(TicketNotFoundException.class, () -> troubleTicketService.closeTroubleTicket(TICKET_ID, request));

        verify(troubleTicketRepository, never()).save(any());
    }

    @Test
    void shouldReturnAddedNoteWhenAddingNoteToTicket() {
        TroubleTicketEntity ticket = new TroubleTicketEntity(
                TENANT_ID,
                "f40ef04b-ad43-4dc5-aad8-c7b6a0e021ec",
                322,
                "new summary for the ticket",
                TroubleTicketStatus.NEW);
        ticket.addNote("first note");
        String firstNoteId = ticket.getNotes().getFirst().getId();

        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.of(ticket));
        Note result = troubleTicketService.addTroubleTicketNote(TICKET_ID, new NoteCreateRequest("second Note"));

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
        when(troubleTicketRepository.findByIdAndTenantId(TICKET_ID, TENANT_ID)).thenReturn(Optional.empty());

        NoteCreateRequest request = new NoteCreateRequest("new note for the ticket");

        assertThrows(
                TicketNotFoundException.class, () -> troubleTicketService.addTroubleTicketNote(TICKET_ID, request));

        verify(troubleTicketRepository, never()).save(any());
    }
}
