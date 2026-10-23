package com.pharma.erp.manufacturers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create/replace payload for {@code /api/manufacturers}. */
public record ManufacturerRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ManufacturerType type,
        @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9/\\-. ]*$", message = "may contain only letters, digits, spaces and / - .")
                String licenceNumber,
        @Size(max = 255) String contactPerson,
        @Size(max = 500) String address,
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+?[0-9 \\-]{7,20}$", message = "must be 7-20 digits, optionally with +, spaces or -")
                String phone,
        @Size(max = 100) String city,
        @Size(max = 100) String district,
        @Size(max = 100) String state,
        @Pattern(regexp = "^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z][Zz][0-9A-Za-z]$",
                        message = "must be a valid 15-character GSTIN")
                String gstin) {}
