package com.pharma.erp.payments.repository;

import com.pharma.erp.payments.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @EntityGraph(attributePaths = "paymentMethod")
    Page<Payment> findByInvoiceId(Integer invoiceId, Pageable pageable);

    @EntityGraph(attributePaths = "paymentMethod")
    Page<Payment> findAllBy(Pageable pageable);

    boolean existsByPaymentMethodId(Integer paymentMethodId);
}
