package com.pharma.erp.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class ClientApiIntegrationTest extends AbstractIntegrationTest {

    private static final String USER = "clients_it";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns201WithLocationAndNormalisedFields() throws Exception {
        String name = unique("Create");
        postClient("""
                {"name": "  %s  ", "address": "1 MG Road", "phone": " +91 98250 00001 ", "email": "a@b.example",
                 "city": "Pune", "district": "Pune", "state": "Maharashtra", "gstin": "27aapfu0939f1zv",
                 "feedbackEmail": "  "}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/clients/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.phone").value("+91 98250 00001"))
                .andExpect(jsonPath("$.gstin").value("27AAPFU0939F1ZV"))
                .andExpect(jsonPath("$.feedbackEmail").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void writeResponsesReportStoredTimestamps() throws Exception {
        String created = postClient("{\"name\": \"%s\"}".formatted(unique("Timestamps")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode createdNode = objectMapper.readTree(created);
        String fetched = mockMvc.perform(get("/api/clients/{id}", createdNode.get("id").asInt()).with(bearer(USER)))
                .andReturn().getResponse().getContentAsString();
        JsonNode fetchedNode = objectMapper.readTree(fetched);
        assertThat(createdNode.get("createdAt")).isEqualTo(fetchedNode.get("createdAt"));
        assertThat(createdNode.get("updatedAt")).isEqualTo(fetchedNode.get("updatedAt"));
    }

    @Test
    void getReturnsClientAndUnknownIs404() throws Exception {
        int id = createClient(unique("Get"), "0261-1110001");
        mockMvc.perform(get("/api/clients/{id}", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.phone").value("0261-1110001"));
        mockMvc.perform(get("/api/clients/{id}", Integer.MAX_VALUE).with(bearer(USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Client " + Integer.MAX_VALUE + " not found"));
    }

    @Test
    void invalidPayloadIs400ProblemJsonWithFieldErrors() throws Exception {
        postClient("""
                {"name": " ", "phone": "call me", "email": "not-an-email", "gstin": "12345"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.phone").isArray())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.gstin").isArray());
    }

    @Test
    void updateReplacesFieldsAndUnknownIs404() throws Exception {
        int id = createClient(unique("Update"), "+91 90000 00002");
        String renamed = unique("Renamed");
        mockMvc.perform(put("/api/clients/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "city": "Surat", "state": "Gujarat"}
                                """.formatted(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.city").value("Surat"))
                .andExpect(jsonPath("$.phone").doesNotExist());
        mockMvc.perform(put("/api/clients/{id}", Integer.MAX_VALUE).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Nobody\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesClientAndDetachesFeedback() throws Exception {
        int id = createClient(unique("Delete"), null);
        int feedbackId = addFeedback(id, 4, "Fine");
        mockMvc.perform(delete("/api/clients/{id}", id).with(bearer(USER))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/clients/{id}", id).with(bearer(USER))).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/clients/{id}", id).with(bearer(USER))).andExpect(status().isNotFound());
        Integer clientId = jdbc.queryForObject(
                "SELECT client_id FROM feedback WHERE feedback_id = ?", Integer.class, feedbackId);
        assertThat(clientId).isNull();
    }

    @Test
    void deleteClientReferencedByInvoiceIs409() throws Exception {
        int id = createClient(unique("Invoiced"), null);
        jdbc.update("INSERT INTO invoice (client_id, invoice_number, invoice_date) VALUES (?, ?, CURRENT_DATE)",
                id, "T05-" + UUID.randomUUID().toString().substring(0, 18));
        mockMvc.perform(delete("/api/clients/{id}", id).with(bearer(USER)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void searchByNameIsCaseInsensitiveSubstring() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        int match = createClient("Zenith Pharma " + tag, null);
        createClient("Other Store " + UUID.randomUUID(), null);
        mockMvc.perform(get("/api/clients").param("name", "ZENITH pharma " + tag.toUpperCase()).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(match));
    }

    @Test
    void searchByPhoneIgnoresFormatting() throws Exception {
        String digits = String.valueOf(System.nanoTime()).substring(4, 12);
        String formatted = "+91 " + digits.substring(0, 4) + "-" + digits.substring(4);
        int match = createClient(unique("Phone"), formatted);
        mockMvc.perform(get("/api/clients").param("phone", digits).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(match)));
        mockMvc.perform(get("/api/clients").param("phone", digits.substring(0, 4) + " " + digits.substring(4))
                        .with(bearer(USER)))
                .andExpect(jsonPath("$.content[*].id", hasItem(match)));
    }

    @Test
    void searchWithQMatchesNameOrPhoneAndLikeWildcardsAreLiteral() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        int byName = createClient("Q Match " + tag, null);
        mockMvc.perform(get("/api/clients").param("q", tag).with(bearer(USER)))
                .andExpect(jsonPath("$.content[*].id", hasItem(byName)));
        mockMvc.perform(get("/api/clients").param("name", "%").param("size", "200").with(bearer(USER)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(byName))));
    }

    @Test
    void listIsPaginatedAndSortableWithUnknownSortRejected() throws Exception {
        mockMvc.perform(get("/api/clients").param("size", "2").param("sort", "name,desc").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content.length()").value(2));
        mockMvc.perform(get("/api/clients").param("sort", "password").with(bearer(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.sort").isArray());
    }

    @Test
    void demoSeedIsPresent() throws Exception {
        mockMvc.perform(get("/api/clients").param("name", "Shree Ganesh Medical Stores").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].city").value("Ahmedabad"));
        Integer seededFeedback = jdbc.queryForObject("""
                SELECT COUNT(*) FROM feedback f JOIN client c ON c.client_id = f.client_id
                WHERE c.client_name = 'Shree Ganesh Medical Stores'""", Integer.class);
        assertThat(seededFeedback).isGreaterThanOrEqualTo(2);
    }

    @Test
    void feedbackCanBePostedListedAndSummarised() throws Exception {
        int id = createClient(unique("Feedback"), null);
        mockMvc.perform(post("/api/clients/{id}/feedback", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5, \"comment\": \"  Excellent service  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/clients/" + id + "/feedback/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.clientId").value(id))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent service"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        int second = addFeedback(id, 2, "Late delivery");

        mockMvc.perform(get("/api/clients/{id}/feedback", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[0].comment").value("Late delivery"));

        mockMvc.perform(get("/api/clients/{id}/feedback/summary", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(id))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.averageRating").value(3.5));
    }

    @Test
    void createdFeedbackLocationResolvesToThatFeedbackOnly() throws Exception {
        int id = createClient(unique("FeedbackLocation"), null);
        int other = createClient(unique("FeedbackOther"), null);
        String location = mockMvc.perform(post("/api/clients/{id}/feedback", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\": 4, \"comment\": \"Prompt\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        int feedbackId = Integer.parseInt(location.substring(location.lastIndexOf('/') + 1));
        assertThat(location).endsWith("/api/clients/" + id + "/feedback/" + feedbackId);

        mockMvc.perform(get(location).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedbackId))
                .andExpect(jsonPath("$.clientId").value(id))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Prompt"));
        mockMvc.perform(get("/api/clients/{id}/feedback/{fid}", other, feedbackId).with(bearer(USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/clients/{id}/feedback/{fid}", id, Integer.MAX_VALUE).with(bearer(USER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void summaryWithoutFeedbackHasZeroCountAndNoAverage() throws Exception {
        int id = createClient(unique("NoFeedback"), null);
        mockMvc.perform(get("/api/clients/{id}/feedback/summary", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.averageRating").doesNotExist());
    }

    @Test
    void feedbackValidatesRatingAndComment() throws Exception {
        int id = createClient(unique("BadFeedback"), null);
        for (String body : new String[] {
                "{\"rating\": 0, \"comment\": \"x\"}",
                "{\"rating\": 6, \"comment\": \"x\"}",
                "{\"comment\": \"x\"}"}) {
            mockMvc.perform(post("/api/clients/{id}/feedback", id).with(bearer(USER))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.rating").isArray());
        }
        mockMvc.perform(post("/api/clients/{id}/feedback", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\": 3, \"comment\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.comment").isArray());
    }

    @Test
    void feedbackForUnknownClientIs404() throws Exception {
        mockMvc.perform(post("/api/clients/{id}/feedback", Integer.MAX_VALUE).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\": 3, \"comment\": \"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/clients/{id}/feedback", Integer.MAX_VALUE).with(bearer(USER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void feedbackLinksExistingStaffAndRejectsUnknownStaff() throws Exception {
        int id = createClient(unique("StaffFeedback"), null);
        String staffName = "Feedback " + UUID.randomUUID();
        jdbc.update("INSERT INTO staff (first_name) VALUES (?)", staffName);
        Integer employeeId = jdbc.queryForObject(
                "SELECT employee_id FROM staff WHERE first_name = ?", Integer.class, staffName);
        mockMvc.perform(post("/api/clients/{id}/feedback", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 4, \"comment\": \"Helpful\", \"employeeId\": %d}".formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employeeId));
        mockMvc.perform(post("/api/clients/{id}/feedback", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 4, \"comment\": \"Helpful\", \"employeeId\": 2147483646}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.employeeId").isArray());
    }

    private ResultActions postClient(String json) throws Exception {
        return mockMvc.perform(post("/api/clients").with(bearer(USER))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private int createClient(String name, String phone) throws Exception {
        String phoneJson = phone == null ? "null" : "\"" + phone + "\"";
        String body = postClient("{\"name\": \"%s\", \"phone\": %s}".formatted(name, phoneJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asInt();
    }

    private int addFeedback(int clientId, int rating, String comment) throws Exception {
        String body = mockMvc.perform(post("/api/clients/{id}/feedback", clientId).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": %d, \"comment\": \"%s\"}".formatted(rating, comment)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("id").asInt();
    }

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID();
    }
}
