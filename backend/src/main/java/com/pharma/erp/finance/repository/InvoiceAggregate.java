package com.pharma.erp.finance.repository;

import java.math.BigDecimal;

public record InvoiceAggregate(
        Long count, BigDecimal subtotal, BigDecimal gstAmount, BigDecimal totalAmount, BigDecimal paidAmount) {

    public InvoiceAggregate {
        count = count == null ? 0L : count;
        subtotal = Aggregates.orZero(subtotal);
        gstAmount = Aggregates.orZero(gstAmount);
        totalAmount = Aggregates.orZero(totalAmount);
        paidAmount = Aggregates.orZero(paidAmount);
    }
}
