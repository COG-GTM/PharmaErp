package com.pharma.erp.finance.dto;

import java.math.BigDecimal;

public record GstBreakdownEntry(
        BigDecimal gstRate,
        BigDecimal taxableAmount,
        BigDecimal cgstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstRate,
        BigDecimal sgstAmount,
        BigDecimal gstAmount) {}
