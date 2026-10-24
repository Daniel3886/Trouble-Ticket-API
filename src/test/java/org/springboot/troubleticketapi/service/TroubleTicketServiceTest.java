package org.springboot.troubleticketapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateRequest;
import org.springboot.troubleticketapi.dto.TroubleTicketCreateStatus;
import org.springboot.troubleticketapi.entity.TroubleTicketEntity;
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
                        "OK-123456",
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

}
