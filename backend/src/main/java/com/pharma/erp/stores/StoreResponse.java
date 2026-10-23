package com.pharma.erp.stores;

import java.time.Instant;

public record StoreResponse(
        Integer id,
        String code,
        String name,
        String address,
        String gstin,
        Integer managerStaffId,
        Instant createdAt,
        Instant updatedAt) {

    static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                store.getAddress(),
                store.getGstin(),
                store.getManagerStaffId(),
                store.getCreatedAt(),
                store.getUpdatedAt());
    }
}
