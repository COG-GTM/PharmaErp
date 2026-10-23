package com.pharma.erp.clients;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Feedback from a client: rating 1-5, a comment, and optionally the staff member who served them. */
public record FeedbackRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 2000) String comment,
        @Positive Integer employeeId) {

    public FeedbackRequest {
        comment = comment == null ? null : comment.trim();
    }
}
