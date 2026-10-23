package com.pharma.erp.finance.repository;

import java.math.BigDecimal;

public record ExpenseCategoryAggregate(String category, Long count, BigDecimal amount, BigDecimal gstAmount) {

    public ExpenseCategoryAggregate {
        count = count == null ? 0L : count;
        amount = Aggregates.orZero(amount);
        gstAmount = Aggregates.orZero(gstAmount);
    }
}
