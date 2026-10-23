package com.pharma.erp.finance.service;

import com.pharma.erp.finance.dto.ExpenseResponse;
import com.pharma.erp.finance.dto.GstBreakdownEntry;
import com.pharma.erp.finance.dto.InvoiceLineResponse;
import com.pharma.erp.finance.dto.InvoiceResponse;
import com.pharma.erp.finance.dto.InvoiceSummaryResponse;
import com.pharma.erp.finance.entity.Expense;
import com.pharma.erp.finance.entity.Invoice;
import com.pharma.erp.finance.entity.InvoiceLine;
import com.pharma.erp.finance.service.GstCalculator.GstSplit;
import java.math.BigDecimal;

/** Entity -> DTO mapping. CGST/SGST are re-derived from the stored taxable value and rate. */
public final class FinanceMapper {

    private FinanceMapper() {}

    public static GstSplit lineSplit(InvoiceLine line) {
        BigDecimal taxable = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
        return GstCalculator.derive(taxable, line.getGstRate());
    }

    public static InvoiceLineResponse toLineResponse(InvoiceLine line) {
        GstSplit split = lineSplit(line);
        return new InvoiceLineResponse(
                line.getId(), line.getItemId(), line.getDescription(), line.getQuantity(), line.getUnitPrice(),
                split.taxableAmount(), split.gstRate(), split.cgstRate(), split.cgstAmount(), split.sgstRate(),
                split.sgstAmount(), line.getGstAmount(), line.getLineTotal());
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        BigDecimal cgst = invoice.getLines().stream()
                .map(l -> lineSplit(l).cgstAmount())
                .reduce(GstCalculator.zero(), BigDecimal::add);
        return new InvoiceResponse(
                invoice.getId(), invoice.getInvoiceNumber(), invoice.getClientId(), invoice.getStoreId(),
                invoice.getPrescriptionId(), invoice.getDescription(), invoice.getStatus(), invoice.getInvoiceDate(),
                invoice.getSubtotal(), cgst, invoice.getGstAmount().subtract(cgst), invoice.getGstAmount(),
                invoice.getTotalAmount(), invoice.getPaidAmount(), balanceDue(invoice), invoice.getCreatedAt(),
                invoice.getLines().stream().map(FinanceMapper::toLineResponse).toList());
    }

    public static InvoiceSummaryResponse toSummary(Invoice invoice) {
        return new InvoiceSummaryResponse(
                invoice.getId(), invoice.getInvoiceNumber(), invoice.getClientId(), invoice.getStoreId(),
                invoice.getStatus(), invoice.getInvoiceDate(), invoice.getSubtotal(), invoice.getGstAmount(),
                invoice.getTotalAmount(), invoice.getPaidAmount(), balanceDue(invoice));
    }

    public static GstBreakdownEntry toBreakdown(GstSplit split) {
        return new GstBreakdownEntry(
                split.gstRate(), split.taxableAmount(), split.cgstRate(), split.cgstAmount(), split.sgstRate(),
                split.sgstAmount(), split.gstAmount());
    }

    public static ExpenseResponse toResponse(Expense expense) {
        GstSplit split = GstCalculator.derive(expense.getAmount(), expense.getGstRate());
        return new ExpenseResponse(
                expense.getId(), expense.getCategory(), expense.getDescription(), expense.getStatus(),
                expense.getAmount(), expense.getGstRate(), split.cgstAmount(), split.sgstAmount(),
                expense.getGstAmount(), expense.getAmount().add(expense.getGstAmount()), expense.getExpenseDate(),
                expense.getInvoiceId(), expense.getStoreId(), expense.getCreatedAt());
    }

    public static BigDecimal balanceDue(Invoice invoice) {
        return invoice.getTotalAmount().subtract(invoice.getPaidAmount()).max(GstCalculator.zero());
    }
}
