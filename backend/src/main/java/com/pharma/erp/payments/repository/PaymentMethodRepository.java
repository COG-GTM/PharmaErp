package com.pharma.erp.payments.repository;

import com.pharma.erp.payments.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {

    boolean existsByMethodNameIgnoreCase(String methodName);

    boolean existsByMethodNameIgnoreCaseAndIdNot(String methodName, Integer id);

    Page<PaymentMethod> findByActive(boolean active, Pageable pageable);
}
