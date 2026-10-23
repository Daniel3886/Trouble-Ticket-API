package org.springboot.troubleticketapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateTroubleTicketRequest(

        @NotBlank
        String externalId,

        @NotNull
        @Positive
        int serviceId,

        @NotBlank
        @Length(min = 1, max = 5000, message = "Description must be between 1 and 5000 characters")
        String description,

        @NotBlank
        @Pattern(regexp = "new", message = "Status must be 'new'")
        @Length(min = 1, max = 20, message = "Status must be between 1 and 20 characters")
        String status,

        @NotBlank
        String note

) {}
