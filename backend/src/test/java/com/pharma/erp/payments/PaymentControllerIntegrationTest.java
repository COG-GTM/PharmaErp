package com.pharma.erp.payments;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String USER = "payments_it";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    int clientId;
    int invoiceId;
    int cashId;

    @BeforeEach
    void setUp() {
        clientId = PaymentsTestData.insertClient(jdbc);
        invoiceId = PaymentsTestData.insertInvoice(jdbc, clientId);
        cashId = jdbc.queryForObject(
                "SELECT payment_method_id FROM payment_method WHERE method_name = 'Cash'", Integer.class);
    }

    @Test
    void recordsPaymentWithReceiptAndInvoiceClient() throws Exception {
        String body = pay(invoiceId, cashId, "150.50", "  counter-7  ")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/payments/")))
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.paymentMethodId").value(cashId))
                .andExpect(jsonPath("$.paymentType").value("Cash"))
                .andExpect(jsonPath("$.amount").value(150.50))
                .andExpect(jsonPath("$.reference").value("counter-7"))
                .andExpect(jsonPath("$.receiptNumber", matchesPattern("RCPT-\\d{8,}")))
                .andExpect(jsonPath("$.paidAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        int id = json.readTree(body).get("id").asInt();

        mockMvc.perform(get("/api/payments/{id}", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value(PaymentsTestDataReceipt.format(id)))
                .andExpect(jsonPath("$.amount").value(150.50));
        Integer stored = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE payment_id = ? AND payment_amount = 150.50 AND client_id = ?",
                Integer.class, id, clientId);
        org.assertj.core.api.Assertions.assertThat(stored).isEqualTo(1);
    }

    @Test
    void amountIsReturnedWithTwoDecimals() throws Exception {
        pay(invoiceId, cashId, "680", null)
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"amount\":680.00")));
    }

    @Test
    void unknownSortPropertyIs400() throws Exception {
        mockMvc.perform(get("/api/payments").param("sort", "nope").with(bearer(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void invoiceWithoutClientAndBlankReferenceAreAllowed() throws Exception {
        int orphanInvoice = PaymentsTestData.insertInvoice(jdbc, null);
        pay(orphanInvoice, cashId, "1", " ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").doesNotExist())
                .andExpect(jsonPath("$.reference").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    void rejectsNonPositiveAmounts(String amount) throws Exception {
        pay(invoiceId, cashId, amount, null)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.amount").isArray());
        assertNoPayments();
    }

    @Test
    void rejectsAmountsWithMoreThanTwoDecimals() throws Exception {
        pay(invoiceId, cashId, "10.005", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").isArray());
    }

    @Test
    void rejectsMissingFieldsAndLongReference() throws Exception {
        mockMvc.perform(post("/api/payments").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"" + "r".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.invoiceId").isArray())
                .andExpect(jsonPath("$.errors.paymentMethodId").isArray())
                .andExpect(jsonPath("$.errors.amount").isArray())
                .andExpect(jsonPath("$.errors.reference").isArray());
    }

    @Test
    void unknownInvoiceOrMethodIs404() throws Exception {
        pay(Integer.MAX_VALUE, cashId, "10", null)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        pay(invoiceId, Integer.MAX_VALUE, "10", null).andExpect(status().isNotFound());
        assertNoPayments();
    }

    @Test
    void inactiveMethodIsConflict() throws Exception {
        int ddId = jdbc.queryForObject(
                "SELECT payment_method_id FROM payment_method WHERE method_name = 'Demand Draft'", Integer.class);
        pay(invoiceId, ddId, "10", null).andExpect(status().isConflict());
        assertNoPayments();
    }

    @Test
    void listsPaymentsForAnInvoiceNewestFirst() throws Exception {
        int upiId = jdbc.queryForObject(
                "SELECT payment_method_id FROM payment_method WHERE method_name = 'UPI'", Integer.class);
        int other = PaymentsTestData.insertInvoice(jdbc, clientId);
        pay(invoiceId, cashId, "100", "first").andExpect(status().isCreated());
        pay(other, cashId, "5", "other invoice").andExpect(status().isCreated());
        pay(invoiceId, upiId, "50.25", "second").andExpect(status().isCreated());
        jdbc.update("UPDATE payment SET paid_at = TIMESTAMPADD(HOUR, -1, paid_at) WHERE reference = 'first' "
                + "AND invoice_id = ?", invoiceId);

        mockMvc.perform(get("/api/payments").param("invoiceId", String.valueOf(invoiceId)).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].reference", contains("second", "first")))
                .andExpect(jsonPath("$.content[*].paymentType", contains("UPI", "Cash")));

        mockMvc.perform(get("/api/payments").param("invoiceId", String.valueOf(invoiceId))
                        .param("size", "1").with(bearer(USER)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/payments").param("size", "1000").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.reference == 'other invoice')]").isNotEmpty());
    }

    @Test
    void unknownPaymentIs404AndPaymentsAreImmutable() throws Exception {
        mockMvc.perform(get("/api/payments/{id}", Integer.MAX_VALUE).with(bearer(USER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/payments/{id}", 1).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/payments")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions pay(int invoice, int method, String amount, String reference) throws Exception {
        String ref = reference == null ? "" : ",\"reference\":\"" + reference + "\"";
        return mockMvc.perform(post("/api/payments").with(bearer(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invoiceId\":" + invoice + ",\"paymentMethodId\":" + method + ",\"amount\":" + amount
                        + ref + "}"));
    }

    private void assertNoPayments() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM payment WHERE invoice_id = ?", Integer.class,
                invoiceId);
        org.assertj.core.api.Assertions.assertThat(count).isZero();
    }

    private static final class PaymentsTestDataReceipt {
        static String format(int id) {
            return String.format("RCPT-%08d", id);
        }
    }
}
