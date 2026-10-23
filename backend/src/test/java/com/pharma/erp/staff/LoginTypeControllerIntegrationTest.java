package com.pharma.erp.staff;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LoginTypeControllerIntegrationTest extends StaffTestSupport {

    @Test
    void seedMigrationProvidesSpecLoginTypes() throws Exception {
        perform(get("/api/login-types").param("size", "100").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItems("jr.Employee", "sr.Employee", "Manager")));
    }

    @Test
    void crudLifecycle() throws Exception {
        String name = "Auditor " + unique();
        perform(post("/api/login-types").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "  " + name + " "))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/login-types/")))
                .andExpect(jsonPath("$.name").value(name));
        int id = createLoginType("Pharmacist " + unique());

        perform(get("/api/login-types/{id}", id).with(admin())).andExpect(status().isOk());

        String renamed = "Chief Pharmacist " + unique();
        perform(put("/api/login-types/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", renamed))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));

        perform(delete("/api/login-types/{id}", id).with(admin())).andExpect(status().isNoContent());
        perform(get("/api/login-types/{id}", id).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void blankNameIsRejected() throws Exception {
        perform(post("/api/login-types").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").isArray());
    }

    @Test
    void duplicateNameIsConflict() throws Exception {
        String name = "Dup " + unique();
        int id = createLoginType(name);
        perform(post("/api/login-types").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name.toUpperCase()))))
                .andExpect(status().isConflict());
        int other = createLoginType("Other " + unique());
        perform(put("/api/login-types/{id}", other).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name))))
                .andExpect(status().isConflict());
        perform(put("/api/login-types/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name))))
                .andExpect(status().isOk());
    }

    @Test
    void loginTypeInUseCannotBeDeleted() throws Exception {
        int id = createLoginType("InUse " + unique());
        Map<String, Object> body = staffBody(unique());
        body.put("loginTypeId", id);
        createStaff(body);
        perform(delete("/api/login-types/{id}", id).with(admin())).andExpect(status().isConflict());
    }

    @Test
    void unknownSortPropertyIsBadRequest() throws Exception {
        perform(get("/api/login-types").param("sort", "password").with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthenticationAndAdminForWrites() throws Exception {
        perform(get("/api/login-types")).andExpect(status().isUnauthorized());
        perform(get("/api/login-types").with(nonAdmin())).andExpect(status().isOk());
        perform(post("/api/login-types").with(nonAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "X" + unique()))))
                .andExpect(status().isForbidden());
    }
}
