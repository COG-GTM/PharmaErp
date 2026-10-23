package com.pharma.erp.finance;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class InvoiceApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    FinanceTestData data;

    @BeforeEach
    void setUp() {
        data = new FinanceTestData(jdbc);
    }

    private JsonNode createInvoice(String body) throws Exception {
        String json = mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    void createsInvoiceWithGstSplitAndTotals() throws Exception {
        int clientId = data.client("Invoice IT client");
        int storeId = data.store();
        int itemId = data.item("Amoxicillin 250mg");

        String body = """
                {"clientId": %d, "storeId": %d, "description": "Counter sale", "invoiceDate": "2026-03-01",
                 "lines": [
                   {"itemId": %d, "quantity": 10, "unitPrice": 25.50, "gstRate": 12},
                   {"description": "Vitamin C", "quantity": 3, "unitPrice": 120.00, "gstRate": 18},
                   {"description": "Syringe", "quantity": 7, "unitPrice": 13.99, "gstRate": 5},
                   {"description": "Cotton", "quantity": 2, "unitPrice": 45, "gstRate": 0}
                 ]}
                """.formatted(clientId, storeId, itemId);

        mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/invoices/")))
                .andExpect(jsonPath("$.invoiceNumber", matchesPattern("INV-\\d{6,}")))
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.invoiceDate").value("2026-03-01"))
                .andExpect(jsonPath("$.subtotal").value(802.93))
                .andExpect(jsonPath("$.cgstAmount").value(50.15))
                .andExpect(jsonPath("$.sgstAmount").value(50.15))
                .andExpect(jsonPath("$.gstAmount").value(100.30))
                .andExpect(jsonPath("$.totalAmount").value(903.23))
                .andExpect(jsonPath("$.paidAmount").value(0.00))
                .andExpect(jsonPath("$.balanceDue").value(903.23))
                .andExpect(jsonPath("$.lines.length()").value(4))
                .andExpect(jsonPath("$.lines[0].itemId").value(itemId))
                .andExpect(jsonPath("$.lines[0].description").value("Amoxicillin 250mg"))
                .andExpect(jsonPath("$.lines[0].taxableAmount").value(255.00))
                .andExpect(jsonPath("$.lines[0].cgstRate").value(6.00))
                .andExpect(jsonPath("$.lines[0].cgstAmount").value(15.30))
                .andExpect(jsonPath("$.lines[0].sgstAmount").value(15.30))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(285.60))
                .andExpect(jsonPath("$.lines[2].cgstAmount").value(2.45))
                .andExpect(jsonPath("$.lines[2].lineTotal").value(102.83))
                .andExpect(jsonPath("$.lines[3].gstAmount").value(0.00));
    }

    @Test
    void getListAndReceiptReturnTheStoredInvoice() throws Exception {
        int clientId = data.client("Receipt IT client");
        JsonNode created = createInvoice("""
                {"clientId": %d, "invoiceDate": "2026-03-02", "lines": [
                   {"description": "Gloves", "quantity": 20, "unitPrice": 349.00, "gstRate": 12},
                   {"description": "Sanitiser", "quantity": 4, "unitPrice": 1125.75, "gstRate": 18},
                   {"description": "Cream", "quantity": 6, "unitPrice": 199.99, "gstRate": 28},
                   {"description": "More gloves", "quantity": 1, "unitPrice": 349.00, "gstRate": 12}
                 ]}
                """.formatted(clientId));
        int id = created.get("id").asInt();
        String number = created.get("invoiceNumber").asText();

        mockMvc.perform(get("/api/invoices/{id}", id).with(bearer("finance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value(number))
                .andExpect(jsonPath("$.lines.length()").value(4))
                .andExpect(jsonPath("$.totalAmount").value(created.get("totalAmount").decimalValue()));

        mockMvc.perform(get("/api/invoices").param("clientId", String.valueOf(clientId)).with(bearer("finance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].balanceDue").value(created.get("totalAmount").decimalValue()))
                .andExpect(jsonPath("$.content[0].lines").doesNotExist());

        // 6980.00 @12 -> 837.60; 4503.00 @18 -> 810.54; 1199.94 @28 -> 335.98; 349.00 @12 -> 41.88
        mockMvc.perform(get("/api/invoices/{id}/receipt", id).with(bearer("finance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("ER-" + number))
                .andExpect(jsonPath("$.invoiceId").value(id))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.lines.length()").value(4))
                .andExpect(jsonPath("$.gstBreakdown.length()").value(3))
                .andExpect(jsonPath("$.gstBreakdown[0].gstRate").value(12.00))
                .andExpect(jsonPath("$.gstBreakdown[0].taxableAmount").value(7329.00))
                .andExpect(jsonPath("$.gstBreakdown[0].cgstAmount").value(439.74))
                .andExpect(jsonPath("$.gstBreakdown[0].sgstAmount").value(439.74))
                .andExpect(jsonPath("$.gstBreakdown[0].gstAmount").value(879.48))
                .andExpect(jsonPath("$.gstBreakdown[2].gstRate").value(28.00))
                .andExpect(jsonPath("$.gstBreakdown[2].cgstRate").value(14.00))
                .andExpect(jsonPath("$.subtotal").value(13031.94))
                .andExpect(jsonPath("$.gstTotal").value(2026.00))
                .andExpect(jsonPath("$.cgstTotal").value(1013.00))
                .andExpect(jsonPath("$.grandTotal").value(15057.94))
                .andExpect(jsonPath("$.balanceDue").value(15057.94))
                .andExpect(jsonPath("$.amountInWords").value(
                        "Rupees Fifteen Thousand Fifty Seven and Ninety Four Paise Only"))
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void listFiltersByDateRangeAndPaginates() throws Exception {
        int storeId = data.store();
        for (String date : new String[] {"2031-05-01", "2031-05-15", "2031-06-01"}) {
            createInvoice("""
                    {"storeId": %d, "invoiceDate": "%s", "lines": [
                       {"description": "Item", "quantity": 1, "unitPrice": 10, "gstRate": 5}]}
                    """.formatted(storeId, date));
        }

        mockMvc.perform(get("/api/invoices").with(bearer("finance"))
                        .param("storeId", String.valueOf(storeId))
                        .param("from", "2031-05-01").param("to", "2031-05-31")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].invoiceDate").value("2031-05-15"));

        mockMvc.perform(get("/api/invoices").with(bearer("finance"))
                        .param("storeId", String.valueOf(storeId)).param("sort", "invoiceDate,asc"))
                .andExpect(jsonPath("$.content[0].invoiceDate").value("2031-05-01"));

        mockMvc.perform(get("/api/invoices").with(bearer("finance")).param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invoiceDateDefaultsToToday() throws Exception {
        JsonNode created = createInvoice("""
                {"lines": [{"description": "Item", "quantity": 1, "unitPrice": 1, "gstRate": 0}]}
                """);
        org.assertj.core.api.Assertions.assertThat(created.get("invoiceDate").asText())
                .isEqualTo(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString());
    }

    @Test
    void rejectsInvalidPayloadsWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lines\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.lines").exists());

        mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"lines": [{"description": "x", "quantity": 0, "unitPrice": -1, "gstRate": 7}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['lines[0].quantity']").exists())
                .andExpect(jsonPath("$.errors['lines[0].unitPrice']").exists())
                .andExpect(jsonPath("$.errors['lines[0].gstRate']", hasItem("must be one of 0, 5, 12, 18, 28")));

        mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"lines": [{"quantity": 1, "unitPrice": 1.001}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['lines[0].unitPrice']").exists())
                .andExpect(jsonPath("$.errors['lines[0].gstRate']").exists());
    }

    @Test
    void rejectsUnknownReferencesAndMissingDescription() throws Exception {
        mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"clientId": 99999999, "storeId": 99999999, "prescriptionId": 99999999, "lines": [
                                   {"itemId": 99999999, "quantity": 1, "unitPrice": 1, "gstRate": 5},
                                   {"quantity": 1, "unitPrice": 1, "gstRate": 5}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.clientId", hasItem("Client 99999999 does not exist")))
                .andExpect(jsonPath("$.errors.storeId").exists())
                .andExpect(jsonPath("$.errors.prescriptionId").exists())
                .andExpect(jsonPath("$.errors['lines[0].itemId']").exists())
                .andExpect(jsonPath("$.errors['lines[1].description']").exists());
    }

    @Test
    void rejectsAmountsBeyondColumnCapacity() throws Exception {
        mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"lines": [{"description": "bulk", "quantity": 1000000, "unitPrice": 10000, "gstRate": 0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors['lines[0]']").exists());

        mockMvc.perform(post("/api/invoices").with(bearer("finance"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"lines": [
                                   {"description": "a", "quantity": 1, "unitPrice": 9000000000, "gstRate": 0},
                                   {"description": "b", "quantity": 1, "unitPrice": 900000000, "gstRate": 18}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lines").exists())
                .andExpect(jsonPath("$.errors['lines[0]']").doesNotExist())
                .andExpect(jsonPath("$.errors['lines[1]']").doesNotExist());
    }

    @Test
    void missingInvoiceIs404() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}", 99999999).with(bearer("finance")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/invoices/{id}/receipt", 99999999).with(bearer("finance")))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/invoices")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/invoices").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
