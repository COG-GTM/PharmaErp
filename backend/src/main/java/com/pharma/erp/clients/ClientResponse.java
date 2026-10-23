package com.pharma.erp.clients;

import java.time.Instant;

public record ClientResponse(
        Integer id,
        String name,
        String address,
        String phone,
        String email,
        String city,
        String district,
        String state,
        String gstin,
        String feedbackEmail,
        Instant createdAt,
        Instant updatedAt) {

    static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getAddress(),
                client.getPhone(),
                client.getEmail(),
                client.getCity(),
                client.getDistrict(),
                client.getState(),
                client.getGstin(),
                client.getFeedbackEmail(),
                client.getCreatedAt(),
                client.getUpdatedAt());
    }
}
