package com.pharma.erp.staff.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be in the format YYYY-MM") String payPeriod,
        @PastOrPresent LocalDate paidOn) {
}
