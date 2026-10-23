package com.pharma.erp.users.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharma.erp.common.test.AbstractIntegrationTest;
import java.util.concurrent.ThreadLocalRandom;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN = "t02admin";

    @Autowired
    JdbcTemplate jdbc;

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    private static String newUsername() {
        return "t02u" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
    }

    private static String body(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/users").with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body(username, password)))
                .andExpect(status().isCreated());
    }

    private String storedPassword(String username) {
        return jdbc.queryForObject("SELECT password FROM `user` WHERE username = ?", String.class, username);
    }

    private RequestPostProcessor nonAdmin() {
        String token = jwtService.issueToken("t02viewer", "user");
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    @Test
    void registerStoresBcryptHashAndReturnsLocation() throws Exception {
        String username = newUsername();
        mockMvc.perform(post("/api/users").with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body(username, "Secure#2024")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/users/" + username))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist());

        String stored = storedPassword(username);
        assertThat(stored).startsWith("$2a$").isNotEqualTo("Secure#2024");
        assertThat(bcrypt.matches("Secure#2024", stored)).isTrue();
    }

    @Test
    void registerTrimsUsernameAndPasswordLikeLegacy() throws Exception {
        String username = newUsername();
        mockMvc.perform(post("/api/users").with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body("  " + username + " ", " parisdkh  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));
        assertThat(bcrypt.matches("parisdkh", storedPassword(username))).isTrue();
    }

    @Test
    void registerDuplicateIsConflict() throws Exception {
        String username = newUsername();
        register(username, "parisdkh");
        mockMvc.perform(post("/api/users").with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body(username, "another1")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void registerInvalidReturnsProblemWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/users").with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body("123456789", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.username[0]").value("must not consist only of digits"))
                .andExpect(jsonPath("$.errors.password[0]").value("must be at least 8 characters long"));
    }

    @Test
    void registerWithMissingFieldsIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users").with(bearer(ADMIN)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username[0]").value("must not be null"))
                .andExpect(jsonPath("$.errors.password[0]").value("must not be null"));
    }

    @Test
    void listIsPagedAndSortedByUsername() throws Exception {
        String username = newUsername();
        register(username, "parisdkh");
        mockMvc.perform(get("/api/users").param("size", "500").with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", Matchers.hasItem(username)))
                .andExpect(jsonPath("$.content[*].password").doesNotExist())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void listRejectsUnknownSortProperty() throws Exception {
        mockMvc.perform(get("/api/users").param("sort", "password").with(bearer(ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsUserOr404() throws Exception {
        String username = newUsername();
        register(username, "parisdkh");
        mockMvc.perform(get("/api/users/{u}", username).with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
        mockMvc.perform(get("/api/users/{u}", "nobody_here1").with(bearer(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void legacySeededPlaintextUserIsVisible() throws Exception {
        String username = "t02l" + ThreadLocalRandom.current().nextInt(1000, 9999);
        jdbc.update("INSERT INTO `user` (username, password) VALUES (?, 'parisdkh')", username);
        mockMvc.perform(get("/api/users/{u}", username).with(bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void changePasswordRehashes() throws Exception {
        String username = newUsername();
        register(username, "parisdkh");
        mockMvc.perform(put("/api/users/{u}/password", username).with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"newSecret9\"}"))
                .andExpect(status().isNoContent());
        String stored = storedPassword(username);
        assertThat(bcrypt.matches("newSecret9", stored)).isTrue();
        assertThat(bcrypt.matches("parisdkh", stored)).isFalse();
    }

    @Test
    void changePasswordValidatesAndReturns404ForUnknownUser() throws Exception {
        String username = newUsername();
        register(username, "parisdkh");
        mockMvc.perform(put("/api/users/{u}/password", username).with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"has space1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password[0]").value("must not contain spaces"));
        mockMvc.perform(put("/api/users/{u}/password", "nobody_here2").with(bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"newSecret9\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesUserThen404() throws Exception {
        String username = newUsername();
        register(username, "parisdkh");
        mockMvc.perform(delete("/api/users/{u}", username).with(bearer(ADMIN)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/{u}", username).with(bearer(ADMIN)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/users/{u}", username).with(bearer(ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteOwnAccount() throws Exception {
        String username = newUsername();
        register(username, "parisdkh");
        mockMvc.perform(delete("/api/users/{u}", username).with(bearer(username)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/users/{u}", username).with(bearer(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body(newUsername(), "parisdkh")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAdminScope() throws Exception {
        mockMvc.perform(get("/api/users").with(nonAdmin()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(post("/api/users").with(nonAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(body(newUsername(), "parisdkh")))
                .andExpect(status().isForbidden());
    }
}
