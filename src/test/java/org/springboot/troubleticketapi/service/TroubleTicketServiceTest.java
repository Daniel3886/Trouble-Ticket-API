package org.springboot.troubleticketapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springboot.troubleticketapi.dto.CreateTroubleTicketRequest;
import org.springboot.troubleticketapi.entity.TroubleTicket;
import org.springboot.troubleticketapi.repository.TroubleTicketRepository;

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
        CreateTroubleTicketRequest request =
                new CreateTroubleTicketRequest(
                        "OK-123456",
                        987654321,
                        "No data connection",
                        "new",
                        "Initial note"
                );

        when(troubleTicketRepository.save(any(TroubleTicket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = troubleTicketService.createTroubleTicket(request);


        assertEquals("OK-123456", response.externalId());
        assertEquals(987654321L, response.serviceId());
        assertEquals("No data connection", response.description());
        assertEquals("new", response.status());
        assertEquals("Initial note", response.notes().getFirst().note());

        verify(troubleTicketRepository).save(any(TroubleTicket.class));
    }

    @Test
    void shouldRejectInvalidStatus() {
        CreateTroubleTicketRequest request =
                new CreateTroubleTicketRequest(
                        "OK-123456",
                        987654321,
                        "No data connection",
                        "closed",
                        "Initial note"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> troubleTicketService.createTroubleTicket(request)
        );


        verify(troubleTicketRepository, never()).save(any(TroubleTicket.class));
    }

}