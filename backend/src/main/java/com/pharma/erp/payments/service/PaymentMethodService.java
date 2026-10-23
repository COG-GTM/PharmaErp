package com.pharma.erp.payments.service;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.payments.dto.PaymentMethodRequest;
import com.pharma.erp.payments.dto.PaymentMethodResponse;
import com.pharma.erp.payments.entity.PaymentMethod;
import com.pharma.erp.payments.repository.PaymentMethodRepository;
import com.pharma.erp.payments.repository.PaymentRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentMethodService {

    private static final Sort BY_NAME = Sort.by("methodName");

    private final PaymentMethodRepository methods;
    private final PaymentRepository payments;

    public PaymentMethodService(PaymentMethodRepository methods, PaymentRepository payments) {
        this.methods = methods;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> list(Boolean active) {
        List<PaymentMethod> result = active == null ? methods.findAll(BY_NAME) : methods.findByActive(active, BY_NAME);
        return result.stream().map(PaymentMethodResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentMethodResponse get(Integer id) {
        return PaymentMethodResponse.from(find(id));
    }

    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        String name = request.methodName().trim();
        if (methods.existsByMethodNameIgnoreCase(name)) {
            throw duplicate(name);
        }
        PaymentMethod method = new PaymentMethod(name, request.active() == null || request.active());
        return PaymentMethodResponse.from(methods.saveAndFlush(method));
    }

    @Transactional
    public PaymentMethodResponse update(Integer id, PaymentMethodRequest request) {
        PaymentMethod method = find(id);
        String name = request.methodName().trim();
        if (methods.existsByMethodNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicate(name);
        }
        method.setMethodName(name);
        if (request.active() != null) {
            method.setActive(request.active());
        }
        return PaymentMethodResponse.from(methods.saveAndFlush(method));
    }

    @Transactional
    public void delete(Integer id) {
        PaymentMethod method = find(id);
        if (payments.existsByPaymentMethodId(id)) {
            throw new ConflictException(
                    "Payment method " + id + " is used by recorded payments; deactivate it instead");
        }
        methods.delete(method);
    }

    PaymentMethod find(Integer id) {
        return methods.findById(id).orElseThrow(() -> NotFoundException.of("Payment method", id));
    }

    private static ConflictException duplicate(String name) {
        return new ConflictException("Payment method '" + name + "' already exists");
    }
}
