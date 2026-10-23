package com.pharma.erp.staff.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pharma.erp.staff.entity.StaffType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Create/replace payload for a staff member (spec sheet "Staff Management"). */
public record StaffRequest(
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String middleName,
        @Size(max = 100) String lastName,
        @Past LocalDate dateOfBirth,
        @PastOrPresent LocalDate joinDate,
        @Size(max = 255) String experience,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal salary,
        @Size(max = 255) String homeTown,
        @Size(max = 500) String homeTownAddress,
        @Size(max = 500) String jobAddress,
        @Size(max = 500) String leavingAddress,
        @Pattern(regexp = MOBILE_PATTERN, message = "must be 10-15 digits, optionally prefixed with +")
        String mobileNumber,
        @Email @Size(max = 255) String email,
        @Size(max = 255) String lastCompany,
        @Size(max = 500) String resumePath,
        @Size(max = 1000) String feedback,
        @Size(max = 1000) String suggestion,
        @Positive Integer loginTypeId,
        @Pattern(regexp = USERNAME_PATTERN, message = USERNAME_MESSAGE) String username,
        @Positive Integer storeId,
        StaffType staffType,
        Boolean active) {

    public static final String MOBILE_PATTERN = "^\\+?[0-9]{10,15}$";

    /** Legacy {@code UserValidation} rule: 8-12 word characters, not all digits, no spaces. */
    public static final String USERNAME_PATTERN = "^(?!\\d+$)\\w{8,12}$";

    public static final String USERNAME_MESSAGE =
            "must be 8-12 letters, digits or underscores and not only digits";

    /** Trim before validation, as the legacy {@code UserValidation} does. */
    public StaffRequest {
        username = trim(username);
        email = trim(email);
        mobileNumber = trim(mobileNumber);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    @JsonIgnore
    @AssertTrue(message = "joinDate must be after dateOfBirth")
    public boolean isJoinDateAfterBirth() {
        return dateOfBirth == null || joinDate == null || joinDate.isAfter(dateOfBirth);
    }
}
