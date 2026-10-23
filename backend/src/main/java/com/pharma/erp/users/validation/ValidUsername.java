package com.pharma.erp.users.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Legacy username rules: 8-12 word characters, not all digits, no spaces. See {@link LegacyUserRules}. */
@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

    String message() default "invalid username";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
