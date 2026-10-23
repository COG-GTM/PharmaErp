package com.pharma.erp.payments.repository;

import com.pharma.erp.payments.entity.PaymentMethod;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {

    boolean existsByMethodNameIgnoreCase(String methodName);

    boolean existsByMethodNameIgnoreCaseAndIdNot(String methodName, Integer id);

    List<PaymentMethod> findByActive(boolean active, Sort sort);
}
