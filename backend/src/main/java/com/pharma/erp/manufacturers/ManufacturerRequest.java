package com.pharma.erp.manufacturers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create/replace payload for {@code /api/manufacturers}. Strings are trimmed and blank values become
 * {@code null} before validation, so optional fields can be cleared with {@code ""}.
 */
public record ManufacturerRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ManufacturerType type,
        @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9/\\-. ]*$", message = "may contain only letters, digits, spaces and / - .")
                String licenceNumber,
        @Size(max = 255) String contactPerson,
        @Size(max = 500) String address,
        @Email @Size(max = 255) String email,
        @Size(max = 20)
                @Pattern(regexp = "^\\+?[0-9](?:[ \\-]?[0-9]){6,14}$",
                        message = "must be 7-15 digits, optionally with a leading + and single spaces or -")
                String phone,
        @Size(max = 100) String city,
        @Size(max = 100) String district,
        @Size(max = 100) String state,
        @Pattern(regexp = "^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z][Zz][0-9A-Za-z]$",
                        message = "must be a valid 15-character GSTIN")
                String gstin) {

    public ManufacturerRequest {
        name = clean(name);
        licenceNumber = clean(licenceNumber);
        contactPerson = clean(contactPerson);
        address = clean(address);
        email = clean(email);
        phone = clean(phone);
        city = clean(city);
        district = clean(district);
        state = clean(state);
        gstin = clean(gstin);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
