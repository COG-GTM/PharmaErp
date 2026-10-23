package com.pharma.erp.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** List view of an invoice (no lines). */
public record InvoiceSummaryResponse(
        Integer id,
        String invoiceNumber,
        Integer clientId,
        Integer storeId,
        String status,
        LocalDate invoiceDate,
        BigDecimal subtotal,
        BigDecimal gstAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue) {}
