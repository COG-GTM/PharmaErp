package com.pharma.erp.finance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class ExpenseApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void crudLifecycle() throws Exception {
        int storeId = new FinanceTestData(jdbc).store();
        String created = mockMvc.perform(post("/api/expenses").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"category": "electricityBill", "description": "March power", "amount": 999.00,
                                 "gstRate": 18, "expenseDate": "2026-03-31", "storeId": %d}
                                """.formatted(storeId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(999.00))
                .andExpect(jsonPath("$.gstRate").value(18.00))
                .andExpect(jsonPath("$.cgstAmount").value(89.91))
                .andExpect(jsonPath("$.sgstAmount").value(89.91))
                .andExpect(jsonPath("$.gstAmount").value(179.82))
                .andExpect(jsonPath("$.totalAmount").value(1178.82))
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(created).get("id").asInt();

        mockMvc.perform(get("/api/expenses/{id}", id).with(bearer("finance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("electricityBill"))
                .andExpect(jsonPath("$.description").value("March power"));

        mockMvc.perform(put("/api/expenses/{id}", id).with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"category": "maintenanceBill", "amount": 1000, "status": "PAID",
                                 "expenseDate": "2026-04-01", "storeId": %d}
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("maintenanceBill"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.gstRate").value(0.00))
                .andExpect(jsonPath("$.gstAmount").value(0.00))
                .andExpect(jsonPath("$.totalAmount").value(1000.00))
                .andExpect(jsonPath("$.description").doesNotExist());

        mockMvc.perform(get("/api/expenses").with(bearer("finance"))
                        .param("storeId", String.valueOf(storeId)).param("category", "maintenanceBill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));

        mockMvc.perform(delete("/api/expenses/{id}", id).with(bearer("finance")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/expenses/{id}", id).with(bearer("finance")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/expenses/{id}", id).with(bearer("finance")))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesPayload() throws Exception {
        mockMvc.perform(post("/api/expenses").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"category": " ", "amount": 0, "gstRate": 10, "status": "LOST"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.gstRate").exists())
                .andExpect(jsonPath("$.errors.status").exists());

        mockMvc.perform(post("/api/expenses").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"category": "internetBill", "amount": 10, "invoiceId": 99999999, "storeId": 99999999}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.invoiceId").exists())
                .andExpect(jsonPath("$.errors.storeId").exists());
    }

    @Test
    void linksToAnExistingInvoice() throws Exception {
        String invoice = mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"lines": [{"description": "Freight", "quantity": 1, "unitPrice": 50, "gstRate": 5}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int invoiceId = objectMapper.readTree(invoice).get("id").asInt();

        mockMvc.perform(post("/api/expenses").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"category": "freight", "amount": 50, "gstRate": 5, "invoiceId": %d}
                                """.formatted(invoiceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.gstAmount").value(2.50));
    }

    @Test
    void updatingMissingExpenseIs404() throws Exception {
        mockMvc.perform(put("/api/expenses/{id}", 99999999).with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\": \"x\", \"amount\": 1}"))
                .andExpect(status().isNotFound());
    }
}
