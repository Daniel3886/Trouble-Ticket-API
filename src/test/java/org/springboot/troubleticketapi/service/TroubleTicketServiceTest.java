package org.springboot.troubleticketapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateStatus;
import org.springboot.troubleticketapi.dto.TroubleTicketStatus;
import org.springboot.troubleticketapi.dto.TroubleTicketSummary;
import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
import org.springboot.troubleticketapi.repository.TroubleTicketRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TroubleTicketServiceTest {

    @Mock
    TroubleTicketRepository troubleTicketRepository;

    @InjectMocks
    TroubleTicketService troubleTicketService;

    @Test
    void shouldCreateTicket() {
        TroubleTicketCreateRequest request =
                new TroubleTicketCreateRequest(
                        "OK-123456",
                        987654321,
                        "No data connection",
                        TroubleTicketCreateStatus.NEW,
                        "Initial note"
                );

        when(troubleTicketRepository.save(any(TroubleTicketEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = troubleTicketService.createTroubleTicket(request);


        assertEquals("OK-123456", response.externalId());
        assertEquals(987654321L, response.serviceId());
        assertEquals("No data connection", response.description());
        assertEquals("new", response.status().getValue());
        assertEquals("Initial note", response.notes().getFirst().text());

        verify(troubleTicketRepository).save(any(TroubleTicketEntity.class));
    }

    @Test
    void shouldRejectMissingStatus() {
        TroubleTicketCreateRequest request =
                new TroubleTicketCreateRequest(
                        "49347def-54b1-44b0-890c-0187018f8273",
                        987654321,
                        "No data connection",
                        null,
                        "Initial note"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> troubleTicketService.createTroubleTicket(request)
        );


        verify(troubleTicketRepository, never()).save(any(TroubleTicketEntity.class));
    }

    @Test
    void shouldReturnTwoTroubleTicketSummaries() {

        TroubleTicketEntity ticket1 = new TroubleTicketEntity();
        ticket1.setExternalId("8a56c5af-aeb7-4b6b-b493-25f7e74207f0");
        ticket1.setServiceId(322);
        ticket1.setDescription("new summary for the ticket");
        ticket1.setStatus(TroubleTicketStatus.NEW);

        TroubleTicketEntity ticket2 = new TroubleTicketEntity();
        ticket2.setExternalId("52dc8009-5bb6-4637-89b5-caf213238313");
        ticket2.setServiceId(828);
        ticket2.setDescription("acknowledged summary for the ticket");
        ticket2.setStatus(TroubleTicketStatus.ACKNOWLEDGED);

        when(troubleTicketRepository.findAll())
                .thenReturn(
                        List.of(ticket1, ticket2)
                );

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
    }


    @Test
    void shouldReturnEmptyList() {
        when(troubleTicketRepository.findAll())
                .thenReturn(List.of());

        List<TroubleTicketSummary> result = troubleTicketService.getTroubleAllTickets();

        assertEquals(0, result.size());
    }


}
