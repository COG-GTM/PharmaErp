package com.pharma.erp.finance.service;

import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.finance.dto.ExpenseRequest;
import com.pharma.erp.finance.dto.ExpenseResponse;
import com.pharma.erp.finance.entity.Expense;
import com.pharma.erp.finance.repository.ExpenseRepository;
import com.pharma.erp.finance.repository.FinanceReferenceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    static final String DEFAULT_STATUS = "PENDING";

    private final ExpenseRepository expenses;
    private final FinanceReferenceRepository references;
    private final Clock clock;

    @Autowired
    public ExpenseService(ExpenseRepository expenses, FinanceReferenceRepository references) {
        this(expenses, references, Clock.systemUTC());
    }

    ExpenseService(ExpenseRepository expenses, FinanceReferenceRepository references, Clock clock) {
        this.expenses = expenses;
        this.references = references;
        this.clock = clock;
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        return FinanceMapper.toResponse(expenses.saveAndFlush(expense));
    }

    @Transactional
    public ExpenseResponse update(int id, ExpenseRequest request) {
        Expense expense = load(id);
        apply(expense, request);
        return FinanceMapper.toResponse(expenses.saveAndFlush(expense));
    }

    @Transactional
    public void delete(int id) {
        expenses.delete(load(id));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(int id) {
        return FinanceMapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> list(
            String category, String status, Integer storeId, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Expense> spec = Specification.<Expense>where(null)
                .and(InvoiceService.equal("category", InvoiceService.trimToNull(category)))
                .and(InvoiceService.equal("status", status))
                .and(InvoiceService.equal("storeId", storeId))
                .and(from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from))
                .and(to == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to));
        return expenses.findAll(spec, pageable).map(FinanceMapper::toResponse);
    }

    private void apply(Expense expense, ExpenseRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (request.invoiceId() != null && !references.invoiceExists(request.invoiceId())) {
            InvoiceService.addError(errors, "invoiceId", "Invoice " + request.invoiceId() + " does not exist");
        }
        if (request.storeId() != null && !references.storeExists(request.storeId())) {
            InvoiceService.addError(errors, "storeId", "Store " + request.storeId() + " does not exist");
        }
        if (!errors.isEmpty()) {
            throw new InvalidReferenceException(errors);
        }
        BigDecimal rate = request.gstRate() != null ? request.gstRate() : BigDecimal.ZERO;
        GstCalculator.GstSplit split = GstCalculator.split(request.amount(), rate);
        expense.setCategory(request.category().trim());
        expense.setDescription(InvoiceService.trimToNull(request.description()));
        expense.setStatus(request.status() != null ? request.status() : DEFAULT_STATUS);
        expense.setAmount(split.taxableAmount());
        expense.setGstRate(split.gstRate());
        expense.setGstAmount(split.gstAmount());
        expense.setExpenseDate(request.expenseDate() != null ? request.expenseDate() : LocalDate.now(clock));
        expense.setInvoiceId(request.invoiceId());
        expense.setStoreId(request.storeId());
    }

    private Expense load(int id) {
        return expenses.findById(id).orElseThrow(() -> NotFoundException.of("Expense", id));
    }
}
