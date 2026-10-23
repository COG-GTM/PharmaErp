package com.pharma.erp.finance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Existence checks for rows owned by other modules (clients, stores, prescriptions, inventory items,
 * invoices). Finance references them by id only, so it reads the shared tables directly instead of
 * depending on other modules' code.
 */
@Repository
public class FinanceReferenceRepository {

    private final JdbcTemplate jdbc;

    public FinanceReferenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean clientExists(int id) {
        return exists("SELECT COUNT(*) FROM `client` WHERE `client_id` = ?", id);
    }

    public boolean storeExists(int id) {
        return exists("SELECT COUNT(*) FROM `store` WHERE `store_id` = ?", id);
    }

    public boolean prescriptionExists(int id) {
        return exists("SELECT COUNT(*) FROM `prescription` WHERE `prescription_id` = ?", id);
    }

    public boolean invoiceExists(int id) {
        return exists("SELECT COUNT(*) FROM `invoice` WHERE `invoice_id` = ?", id);
    }

    /** Name of the inventory item, or empty when it does not exist. */
    public Optional<String> itemName(int id) {
        List<String> names = jdbc.queryForList(
                "SELECT `item_name` FROM `inventory_item` WHERE `item_id` = ?", String.class, id);
        return names.stream().findFirst();
    }

    private boolean exists(String sql, int id) {
        Integer count = jdbc.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
