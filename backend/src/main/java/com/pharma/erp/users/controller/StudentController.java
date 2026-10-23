package com.pharma.erp.users.controller;

import com.pharma.erp.common.web.PageResponse;
import com.pharma.erp.users.dto.StudentResponse;
import com.pharma.erp.users.service.StudentService;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read API over the legacy {@code student} table. */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private static final Set<String> SORTABLE = Set.of("id", "firstName", "lastName", "email");

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public PageResponse<StudentResponse> list(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(studentService.list(SortGuard.check(pageable, SORTABLE)));
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable int id) {
        return studentService.get(id);
    }
}
