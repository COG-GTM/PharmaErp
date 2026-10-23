package com.pharma.erp.manufacturers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
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

class ManufacturerControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String USER = "manufacturers_it";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbc;

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String licence() {
        return "LIC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private String body(String name, String type, String licence) throws Exception {
        return objectMapper.writeValueAsString(new ManufacturerRequest(
                name, type == null ? null : ManufacturerType.valueOf(type), licence, "Priya Nair",
                "12 MG Road", "contact@example.com", "+91 98765 43210", "Pune", "Pune", "Maharashtra",
                "27aaacs3456k1z5"));
    }

    private ResultActions postJson(String json) throws Exception {
        return mockMvc.perform(post("/api/manufacturers")
                .with(bearer(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private int create(String name, String type, String licence) throws Exception {
        String response = postJson(body(name, type, licence))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }

    @Test
    void createReturns201WithNormalisedFieldsAndLocation() throws Exception {
        String name = unique("Acme Pharma");
        String lic = licence().toLowerCase();
        postJson(body("  " + name + "  ", "SUPPLIER", lic))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/manufacturers/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("SUPPLIER"))
                .andExpect(jsonPath("$.licenceNumber").value(lic.toUpperCase()))
                .andExpect(jsonPath("$.contactPerson").value("Priya Nair"))
                .andExpect(jsonPath("$.gstin").value("27AAACS3456K1Z5"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getUpdateAndDeleteRoundTrip() throws Exception {
        int id = create(unique("Roundtrip"), "MANUFACTURER", licence());

        String renamed = unique("Renamed");
        String newLicence = licence();
        mockMvc.perform(put("/api/manufacturers/" + id)
                        .with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(renamed, "DISTRIBUTOR", newLicence)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.type").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.licenceNumber").value(newLicence))
                .andExpect(jsonPath("$.updatedAt").exists());

        mockMvc.perform(get("/api/manufacturers/" + id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.type").value("DISTRIBUTOR"));

        mockMvc.perform(delete("/api/manufacturers/" + id).with(bearer(USER)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/manufacturers/" + id).with(bearer(USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unknownIdIs404ForEveryVerb() throws Exception {
        int missing = Integer.MAX_VALUE;
        mockMvc.perform(get("/api/manufacturers/" + missing).with(bearer(USER))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/manufacturers/" + missing + "/stock").with(bearer(USER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/manufacturers/" + missing)
                        .with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(unique("Ghost"), "SUPPLIER", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/manufacturers/" + missing).with(bearer(USER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPayloadIs400ProblemWithFieldErrors() throws Exception {
        String json = """
                {"name": " ", "type": null, "email": "not-an-email", "phone": "abc", "gstin": "123",
                 "licenceNumber": "bad#licence"}
                """;
        postJson(json)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.type").isArray())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.phone").isArray())
                .andExpect(jsonPath("$.errors.gstin").isArray())
                .andExpect(jsonPath("$.errors.licenceNumber").isArray());
    }

    @Test
    void unknownTypeIs400() throws Exception {
        postJson("{\"name\": \"" + unique("Bad type") + "\", \"type\": \"WHOLESALER\"}")
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/manufacturers").param("type", "WHOLESALER").with(bearer(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateNameOrLicenceIs409() throws Exception {
        String name = unique("Dup");
        String lic = licence();
        create(name, "MANUFACTURER", lic);

        postJson(body(name.toUpperCase(), "SUPPLIER", licence())).andExpect(status().isConflict());
        postJson(body(unique("Other"), "SUPPLIER", lic.toLowerCase()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        int other = create(unique("Other"), "SUPPLIER", licence());
        mockMvc.perform(put("/api/manufacturers/" + other)
                        .with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, "SUPPLIER", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateKeepingOwnNameAndLicenceIsAllowed() throws Exception {
        String name = unique("Same");
        String lic = licence();
        int id = create(name, "MANUFACTURER", lic);
        mockMvc.perform(put("/api/manufacturers/" + id)
                        .with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, "SUPPLIER", lic)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SUPPLIER"));
    }

    @Test
    void listFiltersByTypeAndSearchTermAndPaginates() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        create("Filter Dist A " + tag, "DISTRIBUTOR", null);
        create("Filter Dist B " + tag, "DISTRIBUTOR", null);
        create("Filter Supp " + tag, "SUPPLIER", null);

        mockMvc.perform(get("/api/manufacturers").param("q", tag).param("type", "DISTRIBUTOR").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].type", everyItem(is("DISTRIBUTOR"))))
                .andExpect(jsonPath("$.content[0].name").value("Filter Dist A " + tag));

        mockMvc.perform(get("/api/manufacturers").param("q", tag.toUpperCase()).param("size", "1").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void demoSeedContainsEveryType() throws Exception {
        for (ManufacturerType type : ManufacturerType.values()) {
            mockMvc.perform(get("/api/manufacturers").param("type", type.name()).param("size", "100")
                            .with(bearer(USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements", org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }
        mockMvc.perform(get("/api/manufacturers").param("q", "Cipla").with(bearer(USER)))
                .andExpect(jsonPath("$.content[*].licenceNumber", hasItem("MH-MFG-2017-000188")));
    }

    @Test
    void stockListsInventoryItemsFefoAndBlocksDelete() throws Exception {
        int id = create(unique("Stocked"), "MANUFACTURER", licence());
        jdbc.update("INSERT INTO inventory_item (item_name, batch_no, expiry_date, quantity, unit_price, manufacturer_id)"
                + " VALUES ('Paracetamol 500mg', 'B-LATE', '2028-01-31', 10, 2.50, ?)", id);
        jdbc.update("INSERT INTO inventory_item (item_name, batch_no, expiry_date, quantity, unit_price, manufacturer_id)"
                + " VALUES ('Paracetamol 500mg', 'B-EARLY', '2027-03-31', 4, 2.50, ?)", id);

        String json = mockMvc.perform(get("/api/manufacturers/" + id + "/stock").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manufacturerId").value(id))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.totalQuantity").value(14))
                .andExpect(jsonPath("$.items[0].batchNo").value("B-EARLY"))
                .andExpect(jsonPath("$.items[0].expiryDate").value("2027-03-31"))
                .andExpect(jsonPath("$.items[1].batchNo").value("B-LATE"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("totalValue").decimalValue()).isEqualByComparingTo("35.00");

        mockMvc.perform(delete("/api/manufacturers/" + id).with(bearer(USER)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/manufacturers/" + id).with(bearer(USER))).andExpect(status().isOk());
    }

    @Test
    void requiresAuthenticationAndAdminScopeForWrites() throws Exception {
        mockMvc.perform(get("/api/manufacturers")).andExpect(status().isUnauthorized());

        String readOnly = jwtService.issueToken("viewer", "read");
        mockMvc.perform(get("/api/manufacturers").header("Authorization", "Bearer " + readOnly))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/manufacturers")
                        .header("Authorization", "Bearer " + readOnly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(unique("Forbidden"), "SUPPLIER", null)))
                .andExpect(status().isForbidden());
    }
}
