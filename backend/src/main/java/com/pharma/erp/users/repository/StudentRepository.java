package com.pharma.erp.users.repository;

import com.pharma.erp.users.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {}
