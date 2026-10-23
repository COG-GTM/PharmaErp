package com.pharma.erp.stores;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Create/replace payload. Values are trimmed; {@code code} and {@code gstin} are case-insensitive and stored upper-case;
 * a blank {@code gstin}/{@code address} is stored as null.
 */
public record StoreRequest(
        @NotBlank
        @Pattern(regexp = CODE_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be 2-20 letters, digits or '-', starting with a letter or digit")
        String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String address,
        @Pattern(regexp = GSTIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be a 15-character GSTIN, e.g. 27AABCP1234F1Z5")
        String gstin,
        @Positive Integer managerStaffId) {

    static final String CODE_PATTERN = "^\\s*[A-Z0-9][A-Z0-9-]{1,19}\\s*$";

    /** State code, PAN (5 letters, 4 digits, letter), entity number, 'Z', check character; blank allowed. */
    static final String GSTIN_PATTERN = "^\\s*$|^\\s*[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]\\s*$";
}
