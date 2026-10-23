package com.pharma.erp.payments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Use case "Payment Method": a configurable way of paying (cash, card, UPI, ...). */
@Entity
@Table(name = "payment_method")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_method_id")
    private Integer id;

    @Column(name = "method_name", nullable = false, length = 50)
    private String methodName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected PaymentMethod() {
    }

    public PaymentMethod(String methodName, boolean active) {
        this.methodName = methodName;
        this.active = active;
    }

    public Integer getId() {
        return id;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
