package com.pharma.erp.finance.repository;

import java.math.BigDecimal;

public record GstRateAggregate(BigDecimal gstRate, BigDecimal taxableAmount, BigDecimal gstAmount) {

    public GstRateAggregate {
        gstRate = Aggregates.orZero(gstRate);
        taxableAmount = Aggregates.orZero(taxableAmount);
        gstAmount = Aggregates.orZero(gstAmount);
    }
}
