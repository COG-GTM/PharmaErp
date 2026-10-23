package com.pharma.erp.staff.repository;

import com.pharma.erp.staff.entity.Salary;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRepository extends JpaRepository<Salary, Integer> {

    Page<Salary> findByStaffId(Integer staffId, Pageable pageable);

    Optional<Salary> findByIdAndStaffId(Integer id, Integer staffId);
}
