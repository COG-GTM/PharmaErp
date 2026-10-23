package com.pharma.erp.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Integer id,
        String category,
        String description,
        String status,
        BigDecimal amount,
        BigDecimal gstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal gstAmount,
        BigDecimal totalAmount,
        LocalDate expenseDate,
        Integer invoiceId,
        Integer storeId,
        Instant createdAt) {}
