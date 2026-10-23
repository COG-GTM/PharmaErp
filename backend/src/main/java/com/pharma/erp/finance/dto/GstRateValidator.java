package com.pharma.erp.finance.dto;

import com.pharma.erp.finance.service.GstCalculator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class GstRateValidator implements ConstraintValidator<GstRate, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return value == null || GstCalculator.isAllowedRate(value);
    }
}
