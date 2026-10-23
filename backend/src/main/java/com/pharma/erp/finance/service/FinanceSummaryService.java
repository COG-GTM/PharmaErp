package com.pharma.erp.finance.service;

import com.pharma.erp.finance.dto.FinanceSummaryResponse;
import com.pharma.erp.finance.dto.FinanceSummaryResponse.CategoryTotal;
import com.pharma.erp.finance.dto.GstBreakdownEntry;
import com.pharma.erp.finance.repository.ExpenseCategoryAggregate;
import com.pharma.erp.finance.repository.ExpenseRepository;
import com.pharma.erp.finance.repository.InvoiceAggregate;
import com.pharma.erp.finance.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceSummaryService {

    static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final InvoiceRepository invoices;
    private final ExpenseRepository expenses;

    public FinanceSummaryService(InvoiceRepository invoices, ExpenseRepository expenses) {
        this.invoices = invoices;
        this.expenses = expenses;
    }

    @Transactional(readOnly = true)
    public FinanceSummaryResponse summary(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        LocalDate start = from != null ? from : MIN_DATE;
        LocalDate end = to != null ? to : MAX_DATE;

        InvoiceAggregate sales = invoices.aggregate(start, end);
        List<GstBreakdownEntry> byRate = invoices.gstByRate(start, end).stream()
                .map(r -> {
                    BigDecimal half = r.gstAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                    BigDecimal halfRate = r.gstRate().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                    return new GstBreakdownEntry(r.gstRate(), r.taxableAmount(), halfRate, half, halfRate,
                            r.gstAmount().subtract(half), r.gstAmount());
                })
                .toList();
        BigDecimal salesCgst = sales.gstAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        List<ExpenseCategoryAggregate> categories = expenses.aggregateByCategory(start, end);
        long expenseCount = 0;
        BigDecimal expenseAmount = GstCalculator.zero();
        BigDecimal expenseGst = GstCalculator.zero();
        for (ExpenseCategoryAggregate c : categories) {
            expenseCount += c.count();
            expenseAmount = expenseAmount.add(c.amount());
            expenseGst = expenseGst.add(c.gstAmount());
        }

        return new FinanceSummaryResponse(
                from,
                to,
                new FinanceSummaryResponse.Sales(
                        sales.count(), sales.subtotal(), salesCgst, sales.gstAmount().subtract(salesCgst),
                        sales.gstAmount(), sales.totalAmount(), sales.paidAmount(),
                        sales.totalAmount().subtract(sales.paidAmount()).max(GstCalculator.zero())),
                byRate,
                new FinanceSummaryResponse.Expenses(
                        expenseCount, expenseAmount, expenseGst, expenseAmount.add(expenseGst),
                        categories.stream()
                                .map(c -> new CategoryTotal(c.category(), c.count(), c.amount(), c.gstAmount(),
                                        c.amount().add(c.gstAmount())))
                                .toList()),
                sales.gstAmount().subtract(expenseGst),
                sales.subtotal().subtract(expenseAmount));
    }
}
