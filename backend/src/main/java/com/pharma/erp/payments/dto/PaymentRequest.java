package com.pharma.erp.payments.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Records a payment against an invoice (referenced by id) using an active payment method. */
public record PaymentRequest(
        @NotNull @Positive Integer invoiceId,
        @NotNull @Positive Integer paymentMethodId,
        @NotNull @Positive(message = "must be greater than 0") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Size(max = 100) String reference) {
}
