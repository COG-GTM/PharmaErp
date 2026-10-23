package com.pharma.erp.clients;

import java.time.Instant;

public record FeedbackResponse(
        Integer id, Integer clientId, Integer employeeId, Integer rating, String comment, Instant createdAt) {

    static FeedbackResponse from(ClientFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getClientId(),
                feedback.getEmployeeId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt());
    }
}
