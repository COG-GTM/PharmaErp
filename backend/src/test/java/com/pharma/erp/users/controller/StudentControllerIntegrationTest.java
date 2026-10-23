package com.pharma.erp.users.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharma.erp.common.test.AbstractIntegrationTest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class StudentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private int insertStudent(String first, String last, String email) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO student (first_name, last_name, email) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, first);
            ps.setString(2, last);
            ps.setString(3, email);
            return ps;
        }, keys);
        return keys.getKey().intValue();
    }

    @Test
    void listReturnsStudents() throws Exception {
        int id = insertStudent("Ada", "Lovelace", "ada@example.com");
        mockMvc.perform(get("/api/students").param("size", "500").with(bearer("tarang")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].email", Matchers.contains("ada@example.com")))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getByIdReturnsStudentOr404() throws Exception {
        int id = insertStudent("Grace", "Hopper", "grace@example.com");
        mockMvc.perform(get("/api/students/{id}", id).with(bearer("tarang")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("Grace"))
                .andExpect(jsonPath("$.lastName").value("Hopper"))
                .andExpect(jsonPath("$.email").value("grace@example.com"));
        mockMvc.perform(get("/api/students/{id}", Integer.MAX_VALUE).with(bearer("tarang")))
                .andExpect(status().isNotFound());
    }

    @Test
    void sortByLastNameDescendingAndRejectUnknownSort() throws Exception {
        insertStudent("Zed", "Zzzzlast", "z@example.com");
        mockMvc.perform(get("/api/students").param("sort", "lastName,desc").with(bearer("tarang")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Zzzzlast"));
        mockMvc.perform(get("/api/students").param("sort", "nope").with(bearer("tarang")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
    }
}
