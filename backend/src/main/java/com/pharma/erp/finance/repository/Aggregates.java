package com.pharma.erp.finance.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Aggregates {

    private Aggregates() {}

    static BigDecimal orZero(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
