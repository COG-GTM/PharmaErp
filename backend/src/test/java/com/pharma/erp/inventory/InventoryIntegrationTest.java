package com.pharma.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class InventoryIntegrationTest extends AbstractIntegrationTest {

    private static final String USER = "inventory_it";

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    @Test
    void createReadUpdateDeleteRoundTrip() throws Exception {
        int storeId = insertStore();
        int manufacturerId = insertManufacturer();
        String barcode = uniqueBarcode();
        Map<String, Object> body = itemBody(barcode, 120, today.plusDays(400));
        body.put("storeId", storeId);
        body.put("manufacturerId", manufacturerId);
        body.put("status", "arrived");

        MockHttpServletResponse response = mockMvc.perform(post("/api/inventory").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        JsonNode created = json.readTree(response.getContentAsString());
        int id = created.get("id").asInt();
        assertThat(response.getHeader("Location")).endsWith("/api/inventory/" + id);
        assertThat(created.get("quantity").asInt()).isEqualTo(120);
        assertThat(created.get("status").asText()).isEqualTo("ARRIVED");
        assertThat(created.get("stockStatus").asText()).isEqualTo("IN_STOCK");
        assertThat(created.get("storeId").asInt()).isEqualTo(storeId);
        assertThat(created.get("manufacturerId").asInt()).isEqualTo(manufacturerId);
        assertThat(created.get("stockValue").decimalValue()).isEqualByComparingTo("2220.00");
        assertThat(created.get("daysToExpiry").asLong()).isEqualTo(400);
        assertThat(created.hasNonNull("createdAt")).isTrue();

        mockMvc.perform(get("/api/inventory/{id}", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value(barcode))
                .andExpect(jsonPath("$.batchNo").value("B-" + barcode));

        mockMvc.perform(get("/api/inventory").param("barcode", barcode).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));

        body.put("name", "Paracetamol 650mg");
        body.put("unitPrice", 21.75);
        body.put("status", "DELIVERED");
        body.remove("quantity");
        mockMvc.perform(put("/api/inventory/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paracetamol 650mg"))
                .andExpect(jsonPath("$.unitPrice").value(21.75))
                .andExpect(jsonPath("$.quantity").value(120))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        mockMvc.perform(get("/api/inventory/{id}/movements", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].reason").value("STATUS_CHANGE"))
                .andExpect(jsonPath("$.content[0].note").value("Status ARRIVED -> DELIVERED"))
                .andExpect(jsonPath("$.content[0].quantityChange").value(0))
                .andExpect(jsonPath("$.content[1].reason").value("RECEIPT"))
                .andExpect(jsonPath("$.content[1].quantityChange").value(120))
                .andExpect(jsonPath("$.content[1].location").value("Store " + storeId))
                .andExpect(jsonPath("$.content[1].performedBy").value(USER));

        mockMvc.perform(delete("/api/inventory/{id}", id).with(bearer(USER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("120 units on hand")));
        adjust(id, -120, "DAMAGE").andExpect(status().isOk());
        mockMvc.perform(delete("/api/inventory/{id}", id).with(bearer(USER))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/inventory/{id}", id).with(bearer(USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/inventory/{id}/movements", id).with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].note").value("Archived"));
        adjust(id, 5, "RECEIPT").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/inventory").param("barcode", barcode).with(bearer(USER)))
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(jdbc.queryForList("SELECT reason FROM stock_tracking WHERE item_id = ? ORDER BY tracking_id",
                String.class, id)).containsExactly("RECEIPT", "STATUS_CHANGE", "DAMAGE", "STATUS_CHANGE");

        body.put("quantity", 10);
        mockMvc.perform(post("/api/inventory").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void storeMovesNeedEmptyBatchAndDatabaseEnforcesBatchUniqueness() throws Exception {
        int fromStore = insertStore();
        int toStore = insertStore();
        String barcode = uniqueBarcode();
        Map<String, Object> body = itemBody(barcode, 20, today.plusDays(200));
        body.put("storeId", fromStore);
        int id = create(body);

        body.remove("quantity");
        body.put("storeId", toStore);
        mockMvc.perform(put("/api/inventory/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("TRANSFER_OUT")));
        adjust(id, -20, "TRANSFER_OUT").andExpect(status().isOk());
        mockMvc.perform(put("/api/inventory/{id}", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(toStore));

        String duplicate = "INSERT INTO inventory_item (item_name, barcode, batch_no, store_id) VALUES ('Dup', ?, ?, ?)";
        assertThatThrownBy(() -> jdbc.update(duplicate, barcode, "B-" + barcode, toStore))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(duplicate, barcode, "UNASSIGNED-" + barcode, null);
        assertThatThrownBy(() -> jdbc.update(duplicate, barcode, "UNASSIGNED-" + barcode, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void adjustRecordsMovementWithReasonAndRunningQuantity() throws Exception {
        int id = createItem(uniqueBarcode(), 50, today.plusDays(200));

        mockMvc.perform(post("/api/inventory/{id}/adjust", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityChange\": -45, \"reason\": \"SALE\", \"note\": \"POS invoice 17\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.quantity").value(5))
                .andExpect(jsonPath("$.item.stockStatus").value("LOW"))
                .andExpect(jsonPath("$.movement.quantityChange").value(-45))
                .andExpect(jsonPath("$.movement.quantityAfter").value(5))
                .andExpect(jsonPath("$.movement.reason").value("SALE"))
                .andExpect(jsonPath("$.movement.note").value("POS invoice 17"))
                .andExpect(jsonPath("$.movement.performedBy").value(USER));

        adjust(id, -5, "DAMAGE").andExpect(status().isOk())
                .andExpect(jsonPath("$.item.quantity").value(0))
                .andExpect(jsonPath("$.item.stockStatus").value("OUT_OF_STOCK"));

        adjust(id, 30, "RECEIPT").andExpect(status().isOk())
                .andExpect(jsonPath("$.item.quantity").value(30))
                .andExpect(jsonPath("$.item.stockStatus").value("IN_STOCK"));

        mockMvc.perform(get("/api/inventory/{id}/movements", id).with(bearer(USER)))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].reason").value("RECEIPT"))
                .andExpect(jsonPath("$.content[0].quantityAfter").value(30))
                .andExpect(jsonPath("$.content[1].reason").value("DAMAGE"))
                .andExpect(jsonPath("$.content[2].reason").value("SALE"));
        assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_item WHERE item_id = ?", Integer.class, id))
                .isEqualTo(30);
    }

    @Test
    void adjustRejectsOverdrawSignMismatchZeroAndExpiredReceipt() throws Exception {
        int id = createItem(uniqueBarcode(), 10, today.plusDays(90));

        adjust(id, -11, "SALE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Insufficient stock")));
        adjust(id, 5, "SALE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("negative")));
        adjust(id, -5, "RECEIPT").andExpect(status().isBadRequest());
        adjust(id, 0, "CORRECTION").andExpect(status().isBadRequest());
        adjust(id, 1, "STATUS_CHANGE").andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/inventory/{id}/adjust", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        mockMvc.perform(post("/api/inventory/{id}/adjust", id).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityChange\": 3, \"reason\": \"TELEPORT\"}"))
                .andExpect(status().isBadRequest());
        adjust(999_999_999, 1, "RECEIPT").andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_item WHERE item_id = ?", Integer.class, id))
                .isEqualTo(10);

        int expired = createItem(uniqueBarcode(), 8, today.minusDays(1));
        adjust(expired, 5, "RECEIPT").andExpect(status().isConflict());
        adjust(expired, -8, "EXPIRY_WRITE_OFF").andExpect(status().isOk())
                .andExpect(jsonPath("$.item.quantity").value(0))
                .andExpect(jsonPath("$.item.stockStatus").value("EXPIRED"));
    }

    @Test
    void expiringListsUsableBatchesWithinWindowSoonestFirst() throws Exception {
        int in10 = createItem(uniqueBarcode(), 5, today.plusDays(10));
        int in3 = createItem(uniqueBarcode(), 5, today.plusDays(3));
        int today0 = createItem(uniqueBarcode(), 5, today);
        int in60 = createItem(uniqueBarcode(), 5, today.plusDays(60));
        int expired = createItem(uniqueBarcode(), 5, today.minusDays(2));
        int empty = createItem(uniqueBarcode(), 0, today.plusDays(5));

        JsonNode page = json.readTree(mockMvc.perform(get("/api/inventory/expiring").param("days", "30")
                        .param("size", "500").with(bearer(USER)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        List<Integer> ids = new ArrayList<>();
        page.get("content").forEach(n -> ids.add(n.get("id").asInt()));
        assertThat(ids).contains(today0, in3, in10).doesNotContain(in60, expired, empty);
        assertThat(ids.indexOf(today0)).isLessThan(ids.indexOf(in3));
        assertThat(ids.indexOf(in3)).isLessThan(ids.indexOf(in10));
        page.get("content").forEach(n -> {
            assertThat(LocalDate.parse(n.get("expiryDate").asText())).isBetween(today, today.plusDays(30));
            assertThat(n.get("quantity").asInt()).isPositive();
        });

        mockMvc.perform(get("/api/inventory/expiring").param("days", "-1").with(bearer(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void lowStockUsesThresholdOrPerItemReorderLevel() throws Exception {
        String barcode = uniqueBarcode();
        Map<String, Object> body = itemBody(barcode, 15, today.plusDays(100));
        body.put("reorderLevel", 20);
        int belowOwnLevel = create(body);
        int plenty = createItem(uniqueBarcode(), 500, today.plusDays(100));
        int empty = createItem(uniqueBarcode(), 0, null);
        int expiredLow = createItem(uniqueBarcode(), 1, today.minusDays(3));

        mockMvc.perform(get("/api/inventory/low-stock").param("size", "500").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(belowOwnLevel)))
                .andExpect(jsonPath("$.content[*].id", hasItem(empty)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(plenty))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(expiredLow))));

        mockMvc.perform(get("/api/inventory/low-stock").param("threshold", "10").param("size", "500")
                        .with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(belowOwnLevel))))
                .andExpect(jsonPath("$.content[*].id", hasItem(empty)));

        mockMvc.perform(get("/api/inventory").param("barcode", barcode).param("stockStatus", "LOW")
                        .with(bearer(USER)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/inventory").param("barcode", barcode).param("stockStatus", "IN_STOCK")
                        .with(bearer(USER)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void validationReferencesAndDuplicatesAreRejected() throws Exception {
        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("name", " ");
        invalid.put("quantity", -1);
        invalid.put("gstRate", 150);
        invalid.put("hsnCode", "12");
        mockMvc.perform(post("/api/inventory").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.gstRate").exists())
                .andExpect(jsonPath("$.errors.hsnCode").exists());

        Map<String, Object> badStore = itemBody(uniqueBarcode(), 1, null);
        badStore.put("storeId", 999_999_999);
        mockMvc.perform(post("/api/inventory").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(badStore)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.storeId[0]").value("Store 999999999 does not exist"));

        Map<String, Object> badStatus = itemBody(uniqueBarcode(), 1, null);
        badStatus.put("status", "LOST");
        mockMvc.perform(post("/api/inventory").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(badStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());

        String barcode = uniqueBarcode();
        createItem(barcode, 1, null);
        mockMvc.perform(post("/api/inventory").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(itemBody(barcode, 1, null))))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/inventory/{id}", 999_999_999).with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(itemBody(uniqueBarcode(), 1, null))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/inventory").param("sort", "password").with(bearer(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readsNeedAuthenticationAndWritesNeedAdminScope() throws Exception {
        mockMvc.perform(get("/api/inventory")).andExpect(status().isUnauthorized());
        RequestPostProcessor viewer = request -> {
            request.addHeader("Authorization", "Bearer " + jwtService.issueToken("viewer", "read"));
            return request;
        };
        mockMvc.perform(get("/api/inventory").with(viewer)).andExpect(status().isOk());
        mockMvc.perform(post("/api/inventory").with(viewer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(itemBody(uniqueBarcode(), 1, null))))
                .andExpect(status().isForbidden());
        int id = createItem(uniqueBarcode(), 5, null);
        mockMvc.perform(post("/api/inventory/{id}/adjust", id).with(viewer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityChange\": -1, \"reason\": \"SALE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/inventory/{id}", id).with(viewer)).andExpect(status().isForbidden());
    }

    @Test
    void statusesAndDemoSeedAreMigrated() throws Exception {
        mockMvc.perform(get("/api/inventory/statuses").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("IN_TRANSIT")))
                .andExpect(jsonPath("$[*].name", hasItem("ARRIVED")))
                .andExpect(jsonPath("$[*].name", hasItem("PROCESSING")))
                .andExpect(jsonPath("$[*].name", hasItem("DELIVERED")));
        mockMvc.perform(get("/api/inventory").param("batchNo", "DEMO-CSY-2311").with(bearer(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].stockStatus").value("EXPIRED"));
        mockMvc.perform(get("/api/inventory").param("q", "demo-amx").with(bearer(USER)))
                .andExpect(jsonPath("$.content[0].name").value("Amoxicillin 250mg Capsules"))
                .andExpect(jsonPath("$.content[0].stockStatus").value("LOW"));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_tracking t JOIN inventory_item i ON i.item_id = t.item_id "
                        + "WHERE i.batch_no LIKE 'DEMO-%' AND t.reason = 'RECEIPT'", Integer.class))
                .isGreaterThanOrEqualTo(4);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_tracking t JOIN inventory_item i ON i.item_id = t.item_id "
                        + "WHERE i.batch_no LIKE 'DEMO-%' AND t.quantity_change = 0", Integer.class))
                .isZero();
    }

    private ResultActions adjust(int id, int change, String reason)
            throws Exception {
        return mockMvc.perform(post("/api/inventory/{id}/adjust", id).with(bearer(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityChange\": " + change + ", \"reason\": \"" + reason + "\"}"));
    }

    private int createItem(String barcode, int quantity, LocalDate expiry) throws Exception {
        return create(itemBody(barcode, quantity, expiry));
    }

    private int create(Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/inventory").with(bearer(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asInt();
    }

    private Map<String, Object> itemBody(String barcode, int quantity, LocalDate expiry) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Paracetamol 500mg");
        body.put("description", "Strip of 10");
        body.put("batchNo", "B-" + barcode);
        body.put("expiryDate", expiry == null ? null : expiry.toString());
        body.put("quantity", quantity);
        body.put("unitPrice", 18.50);
        body.put("gstRate", 12.00);
        body.put("hsnCode", "30049099");
        body.put("barcode", barcode);
        return body;
    }

    private int insertStore() {
        String name = "Inventory IT store " + UUID.randomUUID();
        String code = "INV-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO store (store_code, store_name, store_location) VALUES (?, ?, 'Ahmedabad')", code, name);
        return jdbc.queryForObject("SELECT store_id FROM store WHERE store_name = ?", Integer.class, name);
    }

    private int insertManufacturer() {
        String name = "Inventory IT manufacturer " + UUID.randomUUID();
        jdbc.update("INSERT INTO manufacturer (manufacturer_name) VALUES (?)", name);
        return jdbc.queryForObject("SELECT manufacturer_id FROM manufacturer WHERE manufacturer_name = ?",
                Integer.class, name);
    }

    private static String uniqueBarcode() {
        return "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
