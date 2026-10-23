package com.pharma.erp.clients;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Create/replace payload for a client. Values are trimmed before validation; blank optional fields
 * become null and the GSTIN is upper-cased.
 */
public record ClientRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String address,
        @Size(max = 20)
        @Pattern(regexp = "^\\+?[0-9(][0-9 ()\\-.]{5,19}$",
                message = "must be a phone number (digits, spaces, '+', '-', '(', ')', '.')")
        String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 100) String city,
        @Size(max = 100) String district,
        @Size(max = 100) String state,
        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
                message = "must be a 15-character GSTIN, e.g. 27AAPFU0939F1ZV")
        String gstin,
        @Email @Size(max = 255) String feedbackEmail) {

    public ClientRequest {
        name = name == null ? null : name.trim();
        address = clean(address);
        phone = clean(phone);
        email = clean(email);
        city = clean(city);
        district = clean(district);
        state = clean(state);
        gstin = clean(gstin);
        gstin = gstin == null ? null : gstin.toUpperCase(Locale.ROOT);
        feedbackEmail = clean(feedbackEmail);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
