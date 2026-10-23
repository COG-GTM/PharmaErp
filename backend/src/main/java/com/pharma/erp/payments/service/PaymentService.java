package com.pharma.erp.payments.service;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.finance.service.InvoiceSettlementService;
import com.pharma.erp.payments.dto.PaymentRequest;
import com.pharma.erp.payments.dto.PaymentResponse;
import com.pharma.erp.payments.entity.Payment;
import com.pharma.erp.payments.entity.PaymentMethod;
import com.pharma.erp.payments.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final PaymentMethodService methods;
    private final InvoiceLookup invoices;
    private final InvoiceSettlementService settlement;
    private final Clock clock;

    @Autowired
    public PaymentService(PaymentRepository payments, PaymentMethodService methods, InvoiceLookup invoices,
            InvoiceSettlementService settlement) {
        this(payments, methods, invoices, settlement, Clock.systemUTC());
    }

    PaymentService(PaymentRepository payments, PaymentMethodService methods, InvoiceLookup invoices,
            InvoiceSettlementService settlement, Clock clock) {
        this.payments = payments;
        this.methods = methods;
        this.invoices = invoices;
        this.settlement = settlement;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse record(PaymentRequest request) {
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }
        InvoiceLookup.InvoiceRef invoice = invoices.find(request.invoiceId())
                .orElseThrow(() -> NotFoundException.of("Invoice", request.invoiceId()));
        PaymentMethod method = methods.find(request.paymentMethodId());
        if (!method.isActive()) {
            throw new ConflictException("Payment method " + method.getId() + " is inactive");
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        settlement.applyPayment(invoice.id(), amount);
        Payment payment = new Payment(
                invoice.id(),
                invoice.clientId(),
                method,
                amount,
                blankToNull(request.reference()),
                Instant.now(clock).truncatedTo(ChronoUnit.SECONDS));
        payment = payments.saveAndFlush(payment);
        payment.setReceiptNumber(receiptNumber(payment.getId()));
        return PaymentResponse.from(payments.saveAndFlush(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Integer id) {
        return payments.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> NotFoundException.of("Payment", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(Integer invoiceId, Pageable pageable) {
        Page<Payment> page = invoiceId == null
                ? payments.findAllBy(pageable)
                : payments.findByInvoiceId(invoiceId, pageable);
        return PageResponse.from(page, PaymentResponse::from);
    }

    static String receiptNumber(Integer paymentId) {
        return String.format("RCPT-%08d", paymentId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
