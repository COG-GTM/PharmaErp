package com.pharma.erp.payments.dto;

import com.pharma.erp.payments.entity.PaymentMethod;

public record PaymentMethodResponse(Integer id, String methodName, boolean active) {

    public static PaymentMethodResponse from(PaymentMethod method) {
        return new PaymentMethodResponse(method.getId(), method.getMethodName(), method.isActive());
    }
}
