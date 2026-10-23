package com.pharma.erp.staff.repository;

import com.pharma.erp.staff.entity.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginTypeRepository extends JpaRepository<LoginType, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
