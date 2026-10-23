package com.pharma.erp.clients;

/** Aggregate of a client's feedback; {@code averageRating} is null when there is none. */
public record FeedbackSummary(Integer clientId, long count, Double averageRating) {
}
