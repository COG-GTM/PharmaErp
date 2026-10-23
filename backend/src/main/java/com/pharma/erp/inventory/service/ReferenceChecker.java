package com.pharma.erp.inventory.service;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verifies id references to other modules' tables without importing their code (module boundaries
 * are id columns only), so a bad id is a 400 on the offending field rather than a generic FK 409.
 */
@Component
public class ReferenceChecker {

    private record Target(String table, String idColumn, String label) {
    }

    private static final Map<String, Target> TARGETS = Map.of(
            "manufacturerId", new Target("manufacturer", "manufacturer_id", "Manufacturer"),
            "storeId", new Target("store", "store_id", "Store"),
            "employeeId", new Target("staff", "employee_id", "Staff member"),
            "clientId", new Target("client", "client_id", "Client"));

    private final JdbcTemplate jdbc;

    public ReferenceChecker(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void requireExists(String field, Integer id) {
        if (id == null) {
            return;
        }
        Target target = TARGETS.get(field);
        if (target == null) {
            throw new IllegalStateException("Unknown reference field " + field);
        }
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM `" + target.table() + "` WHERE `" + target.idColumn() + "` = ?)",
                Boolean.class, id);
        if (!Boolean.TRUE.equals(exists)) {
            throw new InvalidReferenceException(field, target.label() + " " + id + " does not exist");
        }
    }
}
