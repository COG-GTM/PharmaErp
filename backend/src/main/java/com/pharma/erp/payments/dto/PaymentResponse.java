package com.pharma.erp.payments.dto;

import com.pharma.erp.payments.entity.Payment;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Integer id,
        Integer invoiceId,
        Integer clientId,
        Integer paymentMethodId,
        String paymentType,
        BigDecimal amount,
        String reference,
        String receiptNumber,
        Instant paidAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getClientId(),
                payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().getId(),
                payment.getPaymentType(),
                payment.getPaymentAmount(),
                payment.getReference(),
                payment.getReceiptNumber(),
                payment.getPaidAt());
    }
}
