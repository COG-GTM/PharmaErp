package com.pharma.erp.payments.service;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Read-only, id-based view of the {@code invoice} table owned by the finance module, so payments
 * can validate the invoice and copy its client without depending on finance classes.
 */
@Component
class InvoiceLookup {

    private final JdbcTemplate jdbc;

    InvoiceLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Empty when the invoice does not exist; otherwise its (possibly null) client id. */
    Optional<InvoiceRef> find(Integer invoiceId) {
        return jdbc.query(
                        "SELECT invoice_id, client_id FROM invoice WHERE invoice_id = ?",
                        (rs, row) -> new InvoiceRef(
                                rs.getInt("invoice_id"), rs.getObject("client_id", Integer.class)),
                        invoiceId)
                .stream()
                .findFirst();
    }

    record InvoiceRef(Integer id, Integer clientId) {
    }
}
