package com.pharma.erp.finance.controller;

import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Rejects unknown {@code sort} properties with 400 instead of a query failure. */
final class FinanceSorts {

    static final Set<String> INVOICE_PROPERTIES = Set.of(
            "id", "invoiceNumber", "clientId", "storeId", "status", "invoiceDate", "subtotal", "gstAmount",
            "totalAmount", "paidAmount", "createdAt");
    static final Set<String> EXPENSE_PROPERTIES = Set.of(
            "id", "category", "status", "amount", "gstRate", "gstAmount", "expenseDate", "storeId", "createdAt");

    private FinanceSorts() {}

    static Pageable check(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported sort property '" + order.getProperty() + "'");
            }
        }
        return pageable;
    }
}
