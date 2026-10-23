package com.pharma.erp.staff;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class StaffControllerIntegrationTest extends StaffTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void createReadUpdateDelete() throws Exception {
        String suffix = unique();
        int loginTypeId = createLoginType("Role " + suffix);
        Map<String, Object> body = staffBody(suffix);
        body.put("middleName", "Q");
        body.put("loginTypeId", loginTypeId);
        body.put("email", "Mixed." + suffix + "@Example.com");

        int id = createStaff(body);

        perform(get("/api/staff/{id}", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test" + suffix + " Q Member"))
                .andExpect(jsonPath("$.email").value("mixed." + suffix + "@example.com"))
                .andExpect(jsonPath("$.loginType.id").value(loginTypeId))
                .andExpect(jsonPath("$.staffType").value("INTERNAL"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.salary").value(45000.50))
                .andExpect(jsonPath("$.createdAt").exists());

        body.put("firstName", "Renamed");
        body.put("staffType", "SUPPLIER");
        body.put("active", false);
        body.remove("loginTypeId");
        perform(put("/api/staff/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Renamed"))
                .andExpect(jsonPath("$.staffType").value("SUPPLIER"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.loginType").doesNotExist());

        perform(delete("/api/staff/{id}", id).with(admin())).andExpect(status().isNoContent());
        perform(get("/api/staff/{id}", id).with(admin())).andExpect(status().isNotFound());
        perform(delete("/api/staff/{id}", id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void validationErrorsAreProblemJsonWithFieldMap() throws Exception {
        Map<String, Object> body = staffBody(unique());
        body.put("firstName", "");
        body.put("email", "not-an-email");
        body.put("mobileNumber", "12ab");
        body.put("salary", -1);
        body.put("username", "12345678");
        body.put("dateOfBirth", "2030-01-01");
        perform(post("/api/staff").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.firstName").isArray())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.mobileNumber").isArray())
                .andExpect(jsonPath("$.errors.salary").isArray())
                .andExpect(jsonPath("$.errors.username").isArray())
                .andExpect(jsonPath("$.errors.dateOfBirth").isArray());
    }

    @Test
    void joinDateMustFollowBirthDate() throws Exception {
        Map<String, Object> body = staffBody(unique());
        body.put("dateOfBirth", "2000-01-01");
        body.put("joinDate", "1999-01-01");
        perform(post("/api/staff").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.joinDateAfterBirth").isArray());
    }

    @Test
    void invalidStaffTypeIsBadRequest() throws Exception {
        Map<String, Object> body = staffBody(unique());
        body.put("staffType", "CEO");
        perform(post("/api/staff").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownReferencesAreBadRequest() throws Exception {
        for (Map.Entry<String, Object> ref : Map.<String, Object>of(
                "loginTypeId", 999999, "storeId", 999999, "username", "nouser" + unique().substring(0, 4))
                .entrySet()) {
            Map<String, Object> body = staffBody(unique());
            body.put(ref.getKey(), ref.getValue());
            perform(post("/api/staff").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void linksExistingUserAndStoreAndRejectsDuplicates() throws Exception {
        String username = "stf" + unique().substring(0, 7);
        jdbc.update("INSERT INTO `user` (`username`, `password`) VALUES (?, 'password1')", username);
        jdbc.update("INSERT INTO `store` (`store_name`) VALUES (?)", "Store " + username);
        Integer storeId = jdbc.queryForObject(
                "SELECT store_id FROM store WHERE store_name = ?", Integer.class, "Store " + username);

        String suffix = unique();
        Map<String, Object> body = staffBody(suffix);
        body.put("username", username);
        body.put("storeId", storeId);
        int id = createStaff(body);
        perform(get("/api/staff/{id}", id).with(admin()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.storeId").value(storeId));

        Map<String, Object> sameUser = staffBody(unique());
        sameUser.put("username", username);
        perform(post("/api/staff").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(sameUser)))
                .andExpect(status().isConflict());

        Map<String, Object> sameEmail = staffBody(unique());
        sameEmail.put("email", ("staff." + suffix + "@example.com").toUpperCase());
        perform(post("/api/staff").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(sameEmail)))
                .andExpect(status().isConflict());

        perform(put("/api/staff/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());
    }

    @Test
    void listSupportsPagingFilteringAndSorting() throws Exception {
        String tag = "Zq" + unique();
        int loginTypeId = createLoginType("Filter " + tag);
        for (int i = 0; i < 3; i++) {
            Map<String, Object> body = staffBody(unique());
            body.put("lastName", tag + "_" + i);
            body.put("loginTypeId", loginTypeId);
            body.put("active", i != 2);
            createStaff(body);
        }

        perform(get("/api/staff").param("q", tag).param("size", "2").param("sort", "lastName,desc").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].lastName").value(tag + "_2"))
                .andExpect(jsonPath("$.content[0].loginType.name").value("Filter " + tag));

        perform(get("/api/staff").param("loginTypeId", String.valueOf(loginTypeId)).param("active", "true")
                        .with(admin()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].lastName", not(hasItem(tag + "_2"))));

        perform(get("/api/staff").param("q", "%").param("staffType", "DISTRIBUTOR").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        perform(get("/api/staff").param("sort", "password").with(admin())).andExpect(status().isBadRequest());
    }

    @Test
    void seedMigrationProvidesDemoRoster() throws Exception {
        perform(get("/api/staff").param("q", "@demo.pharmaerp.local").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].loginType.name", hasItem("Manager")));
    }

    @Test
    void writesRequireAdminScope() throws Exception {
        perform(get("/api/staff")).andExpect(status().isUnauthorized());
        perform(get("/api/staff").with(nonAdmin())).andExpect(status().isOk());
        perform(post("/api/staff").with(nonAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(staffBody(unique()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
