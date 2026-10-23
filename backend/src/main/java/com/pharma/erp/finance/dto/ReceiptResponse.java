package com.pharma.erp.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** E-receipt (spec use case "E-receipt") for an invoice: lines, GST summary by rate and totals. */
public record ReceiptResponse(
        String receiptNumber,
        Integer invoiceId,
        String invoiceNumber,
        LocalDate invoiceDate,
        String status,
        Integer clientId,
        Integer storeId,
        String currency,
        List<InvoiceLineResponse> lines,
        List<GstBreakdownEntry> gstBreakdown,
        BigDecimal subtotal,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal gstTotal,
        BigDecimal grandTotal,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        String amountInWords,
        Instant generatedAt) {}
