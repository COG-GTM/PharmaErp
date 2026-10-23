package com.pharma.erp.users.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        LegacyUserRules.UsernameError error = LegacyUserRules.checkUsername(value);
        if (error == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error.message()).addConstraintViolation();
        return false;
    }
}
