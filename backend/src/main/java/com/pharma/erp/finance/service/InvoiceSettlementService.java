package com.pharma.erp.finance.service;

import com.pharma.erp.common.web.ConflictException;
import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.finance.entity.Invoice;
import com.pharma.erp.finance.repository.InvoiceRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finance-owned settlement of invoices by payments. Each payment is applied under a row lock, so
 * {@code paid_amount} always equals the opening paid amount plus the sum of the invoice's payments.
 * Overpayment policy: a payment above the balance due, or against a cancelled invoice, is rejected (409).
 */
@Service
public class InvoiceSettlementService {

    private final InvoiceRepository invoices;

    public InvoiceSettlementService(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    /** Applies {@code amount} to the invoice inside the caller's transaction, updating paid amount and status. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyPayment(Integer invoiceId, BigDecimal amount) {
        Invoice invoice = invoices.findForUpdateById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("Invoice", invoiceId));
        if (Invoice.STATUS_CANCELLED.equals(invoice.getStatus())) {
            throw new ConflictException("Invoice " + invoiceId + " is cancelled");
        }
        BigDecimal balanceDue = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (amount.compareTo(balanceDue) > 0) {
            throw new ConflictException("Payment of " + amount.toPlainString() + " exceeds the balance due of "
                    + balanceDue.max(BigDecimal.ZERO).toPlainString() + " on invoice " + invoiceId);
        }
        BigDecimal paid = invoice.getPaidAmount().add(amount);
        invoice.setPaidAmount(paid);
        invoice.setStatus(paid.compareTo(invoice.getTotalAmount()) >= 0
                ? Invoice.STATUS_PAID
                : Invoice.STATUS_PARTIALLY_PAID);
    }
}
