package com.pharma.erp.manufacturers;

import java.time.Instant;

public record ManufacturerResponse(
        Integer id,
        String name,
        ManufacturerType type,
        String licenceNumber,
        String contactPerson,
        String address,
        String email,
        String phone,
        String city,
        String district,
        String state,
        String gstin,
        Instant createdAt,
        Instant updatedAt) {

    static ManufacturerResponse from(Manufacturer m) {
        return new ManufacturerResponse(
                m.getId(), m.getName(), m.getType(), m.getLicenceNumber(), m.getContactPerson(), m.getAddress(),
                m.getEmail(), m.getPhone(), m.getCity(), m.getDistrict(), m.getState(), m.getGstin(),
                m.getCreatedAt(), m.getUpdatedAt());
    }
}
