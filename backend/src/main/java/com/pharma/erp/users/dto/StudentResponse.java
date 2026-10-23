package com.pharma.erp.users.dto;

import com.pharma.erp.users.entity.Student;

public record StudentResponse(Integer id, String firstName, String lastName, String email) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getFirstName(), student.getLastName(), student.getEmail());
    }
}
