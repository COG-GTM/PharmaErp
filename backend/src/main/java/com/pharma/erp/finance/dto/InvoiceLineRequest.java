package com.pharma.erp.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** One invoice line. {@code description} defaults to the inventory item's name when {@code itemId} is given. */
public record InvoiceLineRequest(
        @Positive Integer itemId,
        @Size(max = 500) String description,
        @NotNull @Positive @Max(1_000_000) Integer quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @NotNull @GstRate BigDecimal gstRate) {}
