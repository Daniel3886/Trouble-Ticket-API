package org.springboot.troubleticketapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;

public record TroubleTicketCreateRequest(

        @NotBlank
        String externalId,

        @NotNull
        @Positive
        long serviceId,

        @NotBlank
        @Length(min = 1, max = 5000, message = "Description must be between 1 and 5000 characters")
        String description,

        @NotNull
        TroubleTicketCreateStatus status,

        @NotBlank
        String note

) {}
