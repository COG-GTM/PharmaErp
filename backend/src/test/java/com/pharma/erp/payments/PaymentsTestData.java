package com.pharma.erp.payments;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** Inserts invoices/clients directly (they belong to other modules) so payment tests own their rows. */
final class PaymentsTestData {

    private PaymentsTestData() {
    }

    static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static int insertClient(JdbcTemplate jdbc) {
        return insert(jdbc, "INSERT INTO client (client_name) VALUES ('" + unique("Client") + "')");
    }

    static int insertInvoice(JdbcTemplate jdbc, Integer clientId) {
        return insert(jdbc, "INSERT INTO invoice (invoice_number, client_id, invoice_date, total_amount) VALUES ('"
                + unique("INV") + "', " + clientId + ", CURRENT_DATE, 5000.00)");
    }

    private static int insert(JdbcTemplate jdbc, String sql) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS), keys);
        return keys.getKey().intValue();
    }
}
