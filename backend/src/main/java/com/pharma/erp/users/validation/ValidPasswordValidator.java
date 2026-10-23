package com.pharma.erp.users.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        LegacyUserRules.PasswordError error = LegacyUserRules.checkPassword(value);
        if (error == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error.message()).addConstraintViolation();
        return false;
    }
}
