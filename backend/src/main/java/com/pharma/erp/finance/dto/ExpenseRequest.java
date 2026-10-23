package com.pharma.erp.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/replace an expense. {@code amount} is the value before GST; GST at {@code gstRate} (default 0) is
 * added on top. {@code status} defaults to PENDING and {@code expenseDate} to today.
 */
public record ExpenseRequest(
        @NotBlank @Size(max = 50) String category,
        @Size(max = 1000) String description,
        @Pattern(regexp = "PENDING|PAID|CANCELLED", message = "must be one of PENDING, PAID, CANCELLED") String status,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @GstRate BigDecimal gstRate,
        LocalDate expenseDate,
        @Positive Integer invoiceId,
        @Positive Integer storeId) {}
