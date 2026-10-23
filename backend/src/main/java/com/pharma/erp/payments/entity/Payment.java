package com.pharma.erp.payments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sheet "Payment" (paymentId, paymentType, paymentAmount, FK clientId) plus use case "E-receipt".
 * Invoices and clients belong to other modules and are referenced by id only.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer id;

    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "invoice_id")
    private Integer invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "receipt_number", length = 32)
    private String receiptNumber;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    protected Payment() {
    }

    public Payment(Integer invoiceId, Integer clientId, PaymentMethod paymentMethod, BigDecimal paymentAmount,
            String reference, Instant paidAt) {
        this.invoiceId = invoiceId;
        this.clientId = clientId;
        this.paymentMethod = paymentMethod;
        this.paymentType = paymentMethod.getMethodName();
        this.paymentAmount = paymentAmount;
        this.reference = reference;
        this.paidAt = paidAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getClientId() {
        return clientId;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public String getReference() {
        return reference;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
