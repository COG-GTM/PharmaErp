package com.pharma.erp.stores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
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
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.ResultActions;

class StoreControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN = "stores_it";

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private ResultActions postStore(Object body) throws Exception {
        return mockMvc.perform(post("/api/stores").with(bearer(ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private JsonNode createStore(String code, String name) throws Exception {
        String body = postStore(Map.of("code", code, "name", name, "address", "1 Test Street"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private int insertStaff() {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> con.prepareStatement(
                "INSERT INTO staff (first_name) VALUES ('Manager')", Statement.RETURN_GENERATED_KEYS), keys);
        return keys.getKey().intValue();
    }

    @Test
    void demoSeedStoresArePresent() throws Exception {
        mockMvc.perform(get("/api/stores").param("q", "PharmaERP").param("size", "50").with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem("MUM-01")))
                .andExpect(jsonPath("$.content[*].code", hasItem("AMD-01")))
                .andExpect(jsonPath("$.content[*].code", hasItem("BLR-WH1")));
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        int managerId = insertStaff();
        String code = unique("ST-");
        String body = postStore(Map.of(
                        "code", code.toLowerCase(),
                        "name", "Store " + code,
                        "address", "22 MG Road, Pune",
                        "gstin", "27aabcp1234f1z5",
                        "managerStaffId", managerId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/stores/")))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.gstin").value("27AABCP1234F1Z5"))
                .andExpect(jsonPath("$.managerStaffId").value(managerId))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        int id = json.readTree(body).get("id").asInt();

        mockMvc.perform(get("/api/stores/{id}", id).with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Store " + code))
                .andExpect(jsonPath("$.address").value("22 MG Road, Pune"));

        mockMvc.perform(put("/api/stores/{id}", id).with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "code", code, "name", "Renamed " + code, "address", "23 MG Road, Pune"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed " + code))
                .andExpect(jsonPath("$.managerStaffId").doesNotExist())
                .andExpect(jsonPath("$.gstin").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT store_name FROM store WHERE store_id = ?", String.class, id))
                .isEqualTo("Renamed " + code);

        mockMvc.perform(delete("/api/stores/{id}", id).with(bearer(ADMIN)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/stores/{id}", id).with(bearer(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void listIsPagedSortedAndSearchable() throws Exception {
        String tag = unique("LST");
        createStore(tag + "-B", "Zeta " + tag);
        createStore(tag + "-A", "Alpha " + tag);

        mockMvc.perform(get("/api/stores").param("q", tag.toLowerCase()).with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alpha " + tag))
                .andExpect(jsonPath("$.content[1].name").value("Zeta " + tag));

        mockMvc.perform(get("/api/stores").param("q", tag).param("sort", "code,desc").param("size", "1")
                        .with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].code").value(tag + "-B"));
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() throws Exception {
        String tag = unique("PCT");
        createStore(tag + "-1", "100% " + tag);
        createStore(tag + "-2", "1000 " + tag);
        createStore(tag + "-3", "A_B " + tag);
        createStore(tag + "-4", "AXB " + tag);

        mockMvc.perform(get("/api/stores").param("q", "100% " + tag).with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value(tag + "-1"));
        mockMvc.perform(get("/api/stores").param("q", "a_b " + tag).with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value(tag + "-3"));
    }

    @Test
    void paddedCodeAndGstinAreTrimmedNotRejected() throws Exception {
        String code = unique("PAD-");
        postStore(Map.of("code", "  " + code.toLowerCase() + " ", "name", " Padded " + code + " ",
                        "gstin", " 27aabcp1234f1z5 "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value("Padded " + code))
                .andExpect(jsonPath("$.gstin").value("27AABCP1234F1Z5"));
    }

    @Test
    void unknownSortPropertyIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/stores").param("sort", "bogus").with(bearer(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sort").exists());
        mockMvc.perform(get("/api/stores").param("q", "x").param("sort", "bogus").with(bearer(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sort").exists());
    }

    @Test
    void validationErrorsAreProblemJson() throws Exception {
        postStore(Map.of("code", "x", "name", " ", "gstin", "NOT-A-GSTIN", "managerStaffId", -1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.gstin").exists())
                .andExpect(jsonPath("$.errors.managerStaffId").exists());
    }

    @Test
    void unknownManagerIsFieldError() throws Exception {
        postStore(Map.of("code", unique("MG-"), "name", unique("Mgr "), "managerStaffId", 999_999))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.managerStaffId[0]").value("staff member 999999 does not exist"));
    }

    @Test
    void duplicateCodeOrNameIsConflict() throws Exception {
        String code = unique("DUP-");
        createStore(code, "Dup " + code);

        postStore(Map.of("code", code.toLowerCase(), "name", "Other " + code))
                .andExpect(status().isConflict());
        postStore(Map.of("code", unique("DUP-"), "name", "Dup " + code))
                .andExpect(status().isConflict());
    }

    @Test
    void updateToAnotherStoresCodeIsConflict() throws Exception {
        JsonNode first = createStore(unique("UPD-"), unique("First "));
        JsonNode second = createStore(unique("UPD-"), unique("Second "));

        mockMvc.perform(put("/api/stores/{id}", second.get("id").asInt()).with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "code", first.get("code").asText(), "name", second.get("name").asText()))))
                .andExpect(status().isConflict());
    }

    @Test
    void missingStoreIsNotFound() throws Exception {
        mockMvc.perform(get("/api/stores/{id}", Integer.MAX_VALUE).with(bearer(ADMIN)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/stores/{id}", Integer.MAX_VALUE).with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", "NOPE", "name", "Nope"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/stores/{id}", Integer.MAX_VALUE).with(bearer(ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingStoreReferencedByInventoryIsConflict() throws Exception {
        int id = createStore(unique("INV-"), unique("Inventory ")).get("id").asInt();
        jdbc.update("INSERT INTO inventory_item (item_name, store_id) VALUES ('Paracetamol', ?)", id);

        mockMvc.perform(delete("/api/stores/{id}", id).with(bearer(ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", not(emptyString())));
        mockMvc.perform(get("/api/stores/{id}", id).with(bearer(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void requiresAuthenticationAndAdminScopeForWrites() throws Exception {
        mockMvc.perform(get("/api/stores")).andExpect(status().isUnauthorized());

        String userToken = jwtService.issueToken("stores_reader", "user");
        mockMvc.perform(get("/api/stores").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/stores").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", unique("RO-"), "name", unique("RO ")))))
                .andExpect(status().isForbidden());
    }
}
