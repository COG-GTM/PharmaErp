package com.pharma.erp.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Finance overview for a date range (cancelled invoices/expenses excluded). {@code netGstPayable} is output
 * GST on sales minus input GST on expenses; {@code netIncome} is sales subtotal minus expense amounts (both
 * before GST).
 */
public record FinanceSummaryResponse(
        LocalDate from,
        LocalDate to,
        Sales sales,
        List<GstBreakdownEntry> salesGstByRate,
        Expenses expenses,
        BigDecimal netGstPayable,
        BigDecimal netIncome) {

    public record Sales(
            long invoiceCount,
            BigDecimal subtotal,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal gstAmount,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount) {}

    public record Expenses(
            long expenseCount,
            BigDecimal amount,
            BigDecimal gstAmount,
            BigDecimal totalAmount,
            List<CategoryTotal> byCategory) {}

    public record CategoryTotal(
            String category, long count, BigDecimal amount, BigDecimal gstAmount, BigDecimal totalAmount) {}
}
