package com.pharma.erp.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Integer id,
        String invoiceNumber,
        Integer clientId,
        Integer storeId,
        Integer prescriptionId,
        String description,
        String status,
        LocalDate invoiceDate,
        BigDecimal subtotal,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal gstAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        Instant createdAt,
        List<InvoiceLineResponse> lines) {}
