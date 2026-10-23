package com.pharma.erp.stores;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Read-only existence check against the staff table, so this module does not depend on the staff module's classes. */
@Component
public class StaffDirectory {

    private final JdbcTemplate jdbc;

    public StaffDirectory(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(int employeeId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM staff WHERE employee_id = ?)", Boolean.class, employeeId);
        return Boolean.TRUE.equals(exists);
    }
}
