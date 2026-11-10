package org.springboot.troubleticketapi.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

record TroubleTicketCreateRequest(
        @NotBlank @Length(max = 255) String externalId,

        @NotNull @Positive Long serviceId,

        @NotBlank @Length(min = 1, max = 5000, message = "Description must be between 1 and 5000 characters")
        String description,

        @NotNull TroubleTicketCreateStatus status,

        @NotBlank @Size(max = 5000, message = "Text must be at most 5000 characters")
        String note) {}
