package com.pharma.erp.auth;

import static com.pharma.erp.auth.AuthTestSupport.payload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.security.JwtProperties;
import com.pharma.erp.common.security.JwtService;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    private String newPlaintextUser(String password) {
        String username = "auth_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO `user` (`username`, `password`) VALUES (?, ?)", username, password);
        return username;
    }

    private String storedPassword(String username) {
        return jdbc.queryForObject("SELECT `password` FROM `user` WHERE `username` = ?", String.class, username);
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void apiLoginReturnsTokenUsernameScopeAndExpiry() throws Exception {
        String username = newPlaintextUser("Secret#123");
        Instant before = Instant.now();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "  " + username + " ", "password", "Secret#123"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.scope").value("admin"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("token").asText();
        JsonNode claims = payload(token);
        assertThat(claims.get("sub").asText()).isEqualTo("user/login");
        assertThat(claims.get("username").asText()).isEqualTo(username);
        Instant expiresAt = Instant.parse(body.get("expiresAt").asText());
        assertThat(expiresAt).isEqualTo(Instant.ofEpochSecond(claims.get("exp").asLong()));
        assertThat(Duration.between(before, expiresAt).toMinutes()).isBetween(59L, 60L);
    }

    @Test
    void apiLoginRejectsBadCredentialsWith401Problem() throws Exception {
        String username = newPlaintextUser("Secret#123");
        for (Map<String, String> creds : List.of(
            Map.of("username", username, "password", "secret#123"),
            Map.of("username", username.toUpperCase(), "password", "Secret#123"),
            Map.of("username", "nobody_" + username, "password", "Secret#123"),
            Map.of("username", "x' OR '1'='1", "password", "y"))) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(creds)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Invalid username or password"))
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }
    }

    @Test
    void apiLoginValidatesBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.username").isArray())
                .andExpect(jsonPath("$.errors.password").isArray());
    }

    @Test
    void legacyPlaintextPasswordIsUpgradedToBcryptOnSuccessfulLogin() throws Exception {
        String username = newPlaintextUser("warehouse88");

        mockMvc.perform(post("/login").param("email", username).param("password", "wrong-one"))
                .andExpect(status().isFound());
        assertThat(storedPassword(username)).isEqualTo("warehouse88");

        mockMvc.perform(post("/login").param("email", username).param("password", "warehouse88"))
                .andExpect(status().isOk());
        String upgraded = storedPassword(username);
        assertThat(upgraded).startsWith("$2").isNotEqualTo("warehouse88");
        assertThat(new BCryptPasswordEncoder().matches("warehouse88", upgraded)).isTrue();

        mockMvc.perform(post("/login").param("email", username).param("password", "warehouse88"))
                .andExpect(status().isOk());
        assertThat(storedPassword(username)).isEqualTo(upgraded);
        mockMvc.perform(post("/login").param("email", username).param("password", "WAREHOUSE88"))
                .andExpect(status().isFound());
    }

    @Test
    void bcryptRowsWrittenByOtherModulesAuthenticate() throws Exception {
        String username = newPlaintextUser("placeholder");
        jdbc.update("UPDATE `user` SET `password` = ? WHERE `username` = ?",
                new BCryptPasswordEncoder().encode("ledger2024"), username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "ledger2024"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void meReturnsCurrentIdentityFromBearerOrCookie() throws Exception {
        String username = newPlaintextUser("Secret#123");
        String token = tokenFor(username);
        Instant exp = jwtService.parse(token).getExpiration().toInstant();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.scope").value("admin"))
                .andExpect(jsonPath("$.expiresAt").value(exp.toString()));
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void meRequiresAValidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExpiresTheTokenCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("token=;")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void tokenReissuesForTheAuthenticatedUserNotAHardCodedOne() throws Exception {
        String username = newPlaintextUser("Secret#123");
        String original = tokenFor(username);

        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
            get("/token").header("Authorization", "Bearer " + original),
            post("/token").cookie(new Cookie("token", original))
        }) {
            MvcResult result = mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn();
            String reissued = objectMapper.readValue(result.getResponse().getContentAsString(), String.class);
            JsonNode claims = payload(reissued);
            assertThat(claims.get("username").asText()).isEqualTo(username);
            assertThat(claims.get("sub").asText()).isEqualTo("user/login");
            assertThat(claims.get("scope").asText()).isEqualTo("admin");
            assertThat(result.getResponse().getCookie("token").getValue()).isEqualTo(reissued);
        }
    }

    @Test
    void tokenDerivesScopeFromTheAccountNotThePresentedToken() throws Exception {
        String username = newPlaintextUser("Secret#123");
        String elevated = jwtService.issueToken(username, "superuser");

        MvcResult result = mockMvc.perform(get("/token").header("Authorization", "Bearer " + elevated))
                .andExpect(status().isOk())
                .andReturn();
        String reissued = objectMapper.readValue(result.getResponse().getContentAsString(), String.class);
        assertThat(payload(reissued).get("scope").asText()).isEqualTo("admin");
    }

    @Test
    void tokenCookieIsHttpOnlySecureAndSameSiteStrict() throws Exception {
        String username = newPlaintextUser("Secret#123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "Secret#123"))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }

    @Test
    void tokenRejectsForeignSignedTokensAndDeletedUsers() throws Exception {
        String username = newPlaintextUser("Secret#123");
        JwtService foreignIssuer = new JwtService(new JwtProperties(null, null, Duration.ofHours(1), "token"));
        mockMvc.perform(get("/token").header("Authorization", "Bearer " + foreignIssuer.issueToken(username, "admin")))
                .andExpect(status().isUnauthorized());

        String token = tokenFor(username);
        jdbc.update("DELETE FROM `user` WHERE `username` = ?", username);
        mockMvc.perform(get("/token").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void bareLegacyLoginRedirectsToLoginPageWithoutError() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/signin"));
    }
}
