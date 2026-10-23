package com.pharma.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharma.erp.common.test.AbstractIntegrationTest;
import com.pharma.erp.finance.service.GstCalculator;
import com.pharma.erp.finance.service.GstCalculator.LineAmounts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class FinanceSummaryIntegrationTest extends AbstractIntegrationTest {

    // A date range no other test writes to, so totals are deterministic on the shared database.
    private static final String FROM = "2098-07-01";
    private static final String TO = "2098-07-31";

    @Autowired
    JdbcTemplate jdbc;

    private void post(String path, String body) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                        .with(bearer("finance")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void summarisesSalesGstAndExpensesForTheRange() throws Exception {
        post("/api/invoices", """
                {"invoiceDate": "2098-07-05", "lines": [
                   {"description": "A", "quantity": 10, "unitPrice": 25.50, "gstRate": 12},
                   {"description": "B", "quantity": 3, "unitPrice": 120.00, "gstRate": 18}]}
                """);
        post("/api/invoices", """
                {"invoiceDate": "2098-07-20", "lines": [
                   {"description": "C", "quantity": 1, "unitPrice": 100.00, "gstRate": 12}]}
                """);
        // Outside the range: ignored.
        post("/api/invoices", """
                {"invoiceDate": "2098-08-01", "lines": [
                   {"description": "D", "quantity": 1, "unitPrice": 5000, "gstRate": 28}]}
                """);
        post("/api/expenses", """
                {"category": "electricityBill", "amount": 400.00, "gstRate": 18, "expenseDate": "2098-07-10"}
                """);
        post("/api/expenses", """
                {"category": "waterSupplierBill", "amount": 50.00, "gstRate": 0, "expenseDate": "2098-07-11"}
                """);
        post("/api/expenses", """
                {"category": "electricityBill", "amount": 100.00, "gstRate": 18, "expenseDate": "2098-07-12"}
                """);
        post("/api/expenses", """
                {"category": "internetBill", "amount": 100.00, "gstRate": 18, "expenseDate": "2098-07-13",
                 "status": "CANCELLED"}
                """);

        // Sales: 255.00 + 360.00 + 100.00 = 715.00; GST 30.60 + 64.80 + 12.00 = 107.40
        // Expenses: 550.00, input GST 72.00 + 18.00 = 90.00
        mockMvc.perform(get("/api/finance/summary").with(bearer("finance")).param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(FROM))
                .andExpect(jsonPath("$.to").value(TO))
                .andExpect(jsonPath("$.sales.invoiceCount").value(2))
                .andExpect(jsonPath("$.sales.subtotal").value(715.00))
                .andExpect(jsonPath("$.sales.cgstAmount").value(53.70))
                .andExpect(jsonPath("$.sales.sgstAmount").value(53.70))
                .andExpect(jsonPath("$.sales.gstAmount").value(107.40))
                .andExpect(jsonPath("$.sales.totalAmount").value(822.40))
                .andExpect(jsonPath("$.sales.paidAmount").value(0.00))
                .andExpect(jsonPath("$.sales.outstandingAmount").value(822.40))
                .andExpect(jsonPath("$.salesGstByRate.length()").value(2))
                .andExpect(jsonPath("$.salesGstByRate[0].gstRate").value(12.00))
                .andExpect(jsonPath("$.salesGstByRate[0].taxableAmount").value(355.00))
                .andExpect(jsonPath("$.salesGstByRate[0].gstAmount").value(42.60))
                .andExpect(jsonPath("$.salesGstByRate[1].gstRate").value(18.00))
                .andExpect(jsonPath("$.salesGstByRate[1].cgstAmount").value(32.40))
                .andExpect(jsonPath("$.expenses.expenseCount").value(3))
                .andExpect(jsonPath("$.expenses.amount").value(550.00))
                .andExpect(jsonPath("$.expenses.gstAmount").value(90.00))
                .andExpect(jsonPath("$.expenses.totalAmount").value(640.00))
                .andExpect(jsonPath("$.expenses.byCategory.length()").value(2))
                .andExpect(jsonPath("$.expenses.byCategory[0].category").value("electricityBill"))
                .andExpect(jsonPath("$.expenses.byCategory[0].count").value(2))
                .andExpect(jsonPath("$.expenses.byCategory[0].amount").value(500.00))
                .andExpect(jsonPath("$.expenses.byCategory[0].totalAmount").value(590.00))
                .andExpect(jsonPath("$.netGstPayable").value(17.40))
                .andExpect(jsonPath("$.netIncome").value(165.00));
    }

    @Test
    void emptyRangeReturnsZeros() throws Exception {
        mockMvc.perform(get("/api/finance/summary").with(bearer("finance"))
                        .param("from", "2097-01-01").param("to", "2097-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales.invoiceCount").value(0))
                .andExpect(jsonPath("$.sales.totalAmount").value(0.00))
                .andExpect(jsonPath("$.expenses.expenseCount").value(0))
                .andExpect(jsonPath("$.netIncome").value(0.00));
    }

    @Test
    void unboundedSummaryIncludesTheDemoSeed() throws Exception {
        mockMvc.perform(get("/api/finance/summary").with(bearer("finance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").doesNotExist())
                .andExpect(jsonPath("$.sales.invoiceCount").isNumber());
    }

    @Test
    void unboundedSummaryIncludesHistoricalEntries() throws Exception {
        int before = expenseCount();
        post("/api/expenses", """
                {"category": "historical", "amount": 10, "expenseDate": "1850-06-15"}
                """);
        assertThat(expenseCount()).isEqualTo(before + 1);
    }

    private int expenseCount() throws Exception {
        String json = mockMvc.perform(get("/api/finance/summary").with(bearer("finance")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.expenses.expenseCount");
    }

    @Test
    void rejectsInvertedRange() throws Exception {
        mockMvc.perform(get("/api/finance/summary").with(bearer("finance"))
                        .param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demoSeedIsConsistentWithTheCalculator() {
        List<Map<String, Object>> invoices = jdbc.queryForList(
                "SELECT invoice_id, invoice_number, subtotal, gst_amount, total_amount FROM invoice "
                        + "WHERE invoice_description LIKE 'Demo:%'");
        assertThat(invoices).hasSize(3);
        for (Map<String, Object> invoice : invoices) {
            int id = ((Number) invoice.get("invoice_id")).intValue();
            assertThat(invoice.get("invoice_number")).isEqualTo(String.format("INV-%06d", id));
            List<LineAmounts> lines = new ArrayList<>();
            for (Map<String, Object> row : jdbc.queryForList(
                    "SELECT quantity, unit_price, gst_rate, gst_amount, line_total FROM invoice_line WHERE invoice_id = ?",
                    id)) {
                LineAmounts line = GstCalculator.line(((Number) row.get("quantity")).intValue(),
                        (BigDecimal) row.get("unit_price"), (BigDecimal) row.get("gst_rate"));
                assertThat(line.gst().gstAmount()).isEqualByComparingTo((BigDecimal) row.get("gst_amount"));
                assertThat(line.lineTotal()).isEqualByComparingTo((BigDecimal) row.get("line_total"));
                lines.add(line);
            }
            GstCalculator.InvoiceTotals totals = GstCalculator.totals(lines);
            assertThat(totals.subtotal()).isEqualByComparingTo((BigDecimal) invoice.get("subtotal"));
            assertThat(totals.gstAmount()).isEqualByComparingTo((BigDecimal) invoice.get("gst_amount"));
            assertThat(totals.total()).isEqualByComparingTo((BigDecimal) invoice.get("total_amount"));
        }
        for (Map<String, Object> expense : jdbc.queryForList(
                "SELECT amount, gst_rate, gst_amount FROM expense WHERE expense_description LIKE 'Demo:%'")) {
            assertThat(GstCalculator.split((BigDecimal) expense.get("amount"), (BigDecimal) expense.get("gst_rate"))
                            .gstAmount())
                    .isEqualByComparingTo((BigDecimal) expense.get("gst_amount"));
        }
    }
}
