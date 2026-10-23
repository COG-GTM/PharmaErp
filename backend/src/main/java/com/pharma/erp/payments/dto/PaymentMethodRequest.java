package com.pharma.erp.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/replace (PUT) body for a payment method; {@code active} defaults to {@code true} when omitted. */
public record PaymentMethodRequest(
        @NotBlank @Size(max = 50) String methodName,
        Boolean active) {
}
