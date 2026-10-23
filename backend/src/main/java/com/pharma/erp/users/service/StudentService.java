package com.pharma.erp.users.service;

import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.users.dto.StudentResponse;
import com.pharma.erp.users.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository students;

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    public Page<StudentResponse> list(Pageable pageable) {
        return students.findAll(pageable).map(StudentResponse::from);
    }

    public StudentResponse get(int id) {
        return students.findById(id).map(StudentResponse::from).orElseThrow(() -> NotFoundException.of("Student", id));
    }
}
