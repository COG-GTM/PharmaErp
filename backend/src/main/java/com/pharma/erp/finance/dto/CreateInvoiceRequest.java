package com.pharma.erp.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** {@code invoiceDate} defaults to today. Client, store and prescription are optional ids. */
public record CreateInvoiceRequest(
        @Positive Integer clientId,
        @Positive Integer storeId,
        @Positive Integer prescriptionId,
        @Size(max = 1000) String description,
        LocalDate invoiceDate,
        @NotEmpty @Size(max = 200) List<@NotNull @Valid InvoiceLineRequest> lines) {}
