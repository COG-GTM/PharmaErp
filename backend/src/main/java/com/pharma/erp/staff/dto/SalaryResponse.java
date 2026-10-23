package com.pharma.erp.staff.dto;

import com.pharma.erp.staff.entity.Salary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryResponse(
        Integer id, Integer staffId, BigDecimal amount, String payPeriod, LocalDate paidOn, Instant createdAt) {

    public static SalaryResponse from(Salary salary, Integer staffId) {
        return new SalaryResponse(
                salary.getId(), staffId, salary.getAmount(), salary.getPayPeriod(), salary.getPaidOn(),
                salary.getCreatedAt());
    }
}
