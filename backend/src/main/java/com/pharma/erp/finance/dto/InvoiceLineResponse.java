package com.pharma.erp.finance.dto;

import java.math.BigDecimal;

public record InvoiceLineResponse(
        Integer id,
        Integer itemId,
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal taxableAmount,
        BigDecimal gstRate,
        BigDecimal cgstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstRate,
        BigDecimal sgstAmount,
        BigDecimal gstAmount,
        BigDecimal lineTotal) {}
