package com.pharma.erp.finance.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A GST slab in percent: 0, 5, 12, 18 or 28. {@code null} is valid (combine with {@code @NotNull}). */
@Documented
@Constraint(validatedBy = GstRateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GstRate {

    String message() default "must be one of 0, 5, 12, 18, 28";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
