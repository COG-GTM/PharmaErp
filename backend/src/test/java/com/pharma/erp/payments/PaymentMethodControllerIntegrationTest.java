package com.pharma.erp.payments;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentMethodControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String USER = "payments_it";

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void listIncludesSeededMethodsAndFiltersByActive() throws Exception {
        mockMvc.perform(get("/api/payment-methods").param("size", "1000").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].methodName", hasItems("Cash", "UPI", "Cheque", "Demand Draft")));
        mockMvc.perform(get("/api/payment-methods").param("active", "true").param("size", "1000").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].methodName", hasItem("Cash")))
                .andExpect(jsonPath("$.content[*].methodName", not(hasItem("Demand Draft"))));
        mockMvc.perform(get("/api/payment-methods").param("active", "false").param("size", "1000").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].methodName", hasItem("Demand Draft")))
                .andExpect(jsonPath("$.content[*].active", not(hasItem(true))));
    }

    @Test
    void listIsPaginatedAndSortedByName() throws Exception {
        mockMvc.perform(get("/api/payment-methods").param("size", "2").param("page", "1").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)));
        mockMvc.perform(get("/api/payment-methods").param("sort", "nope").with(bearer(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void replacingWithoutActiveReactivates() throws Exception {
        String name = PaymentsTestData.unique("Revive");
        int id = create(name);
        mockMvc.perform(put("/api/payment-methods/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + name + "\",\"active\":false}"))
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(put("/api/payment-methods/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createGetUpdateDelete() throws Exception {
        String name = PaymentsTestData.unique("Wallet");
        int id = create(name);

        mockMvc.perform(get("/api/payment-methods/{id}", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodName").value(name))
                .andExpect(jsonPath("$.active").value(true));

        String renamed = PaymentsTestData.unique("EWallet");
        mockMvc.perform(put("/api/payment-methods/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"  " + renamed + "  \",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.methodName").value(renamed))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/payment-methods/{id}", id).with(bearer(USER)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/payment-methods/{id}", id).with(bearer(USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createDefaultsToActiveAndCanStartInactive() throws Exception {
        mockMvc.perform(post("/api/payment-methods").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + PaymentsTestData.unique("Voucher") + "\",\"active\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void duplicateNamesAreConflictsCaseInsensitively() throws Exception {
        String name = PaymentsTestData.unique("Crypto");
        int id = create(name);
        mockMvc.perform(post("/api/payment-methods").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + name.toUpperCase() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        int other = create(PaymentsTestData.unique("Barter"));
        mockMvc.perform(put("/api/payment-methods/{id}", other).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + name + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/payment-methods/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void invalidBodyIs400ProblemWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/payment-methods").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.methodName").isArray());
        mockMvc.perform(post("/api/payment-methods").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + "x".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.methodName").isArray());
    }

    @Test
    void unknownIdsAre404() throws Exception {
        mockMvc.perform(get("/api/payment-methods/{id}", Integer.MAX_VALUE).with(bearer(USER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/payment-methods/{id}", Integer.MAX_VALUE).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + PaymentsTestData.unique("Ghost") + "\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/payment-methods/{id}", Integer.MAX_VALUE).with(bearer(USER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void methodUsedByPaymentsCannotBeDeleted() throws Exception {
        int methodId = create(PaymentsTestData.unique("InUse"));
        int invoiceId = PaymentsTestData.insertInvoice(jdbc, null);
        mockMvc.perform(post("/api/payments").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceId\":" + invoiceId + ",\"paymentMethodId\":" + methodId
                                + ",\"amount\":10}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/payment-methods/{id}", methodId).with(bearer(USER)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/payment-methods/{id}", methodId).with(bearer(USER)))
                .andExpect(status().isOk());
    }

    @Test
    void requiresAuthenticationAndAdminScopeForWrites() throws Exception {
        mockMvc.perform(get("/api/payment-methods")).andExpect(status().isUnauthorized());

        String viewer = jwtService.issueToken(USER, "viewer");
        mockMvc.perform(get("/api/payment-methods").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/payment-methods").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + PaymentsTestData.unique("Nope") + "\"}"))
                .andExpect(status().isForbidden());
    }

    private int create(String name) throws Exception {
        String body = mockMvc.perform(post("/api/payment-methods").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"methodName\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/payment-methods/")))
                .andExpect(jsonPath("$.methodName").value(name))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asInt();
    }
}
