package com.pharma.erp.auth;

import static com.pharma.erp.auth.AuthTestSupport.SEED_USERS;
import static com.pharma.erp.auth.AuthTestSupport.header;
import static com.pharma.erp.auth.AuthTestSupport.payload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** One test per flow in {@code parity/flows.json} (F01-F16), against the identical parity seed. */
class LegacyParityFlowsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void loadParitySeed() {
        SEED_USERS.forEach((username, password) ->
                jdbc.update("REPLACE INTO `user` (`username`, `password`) VALUES (?, ?)", username, password));
    }

    @Test
    void f01_rootRedirectsToLoginPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/signin"));
    }

    @Test
    void f02_redirectRouteSendsBrowserToSpaLogin() throws Exception {
        mockMvc.perform(get("/redirect"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/signin"));
    }

    @Test
    void f03_formPostLoginReturnsJsonStringJwtAndCookie() throws Exception {
        assertLegacySuccess(legacyLogin(post("/login"), "tarang", "parisdkh"), "tarang");
    }

    @Test
    void f04_getQueryStringLoginSucceeds() throws Exception {
        assertLegacySuccess(
                mockMvc.perform(get("/login").param("email", "tarang").param("password", "parisdkh")), "tarang");
    }

    @Test
    void f05_wrongPasswordRedirectsToLoginPage() throws Exception {
        assertLegacyFailure(legacyLogin(post("/login"), "tarang", "wrongpass"));
    }

    @Test
    void f06_unknownUserRedirectsToLoginPage() throws Exception {
        assertLegacyFailure(legacyLogin(post("/login"), "nobody", "whatever1"));
    }

    @Test
    void f07_missingParametersRedirectToLoginPage() throws Exception {
        mockMvc.perform(post("/login"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/signin"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        assertLegacyFailure(mockMvc.perform(post("/login").param("email", "tarang")));
    }

    @Test
    void f08_paddedUsernameIsTrimmed() throws Exception {
        assertLegacySuccess(legacyLogin(post("/login"), "  tarang  ", "parisdkh"), "tarang");
    }

    @Test
    void f09_passwordComparisonIsCaseSensitive() throws Exception {
        assertLegacyFailure(legacyLogin(post("/login"), "tarang", "PARISDKH"));
    }

    @Test
    void f10_quoteInUsernameNoLongerLeaksQueryException() throws Exception {
        assertLegacyFailure(legacyLogin(post("/login"), "x'", "y"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void f11_injectionPayloadDoesNotBypassAuth() throws Exception {
        assertLegacyFailure(legacyLogin(post("/login"), "tarang' OR '1'='1", "anything"));
    }

    @Test
    void f12_seededPharmacistLogsIn() throws Exception {
        assertLegacySuccess(legacyLogin(post("/login"), "pharmacist01", "Secure#2024"), "pharmacist01");
    }

    @Test
    void f13_seededStoreManagerLogsIn() throws Exception {
        assertLegacySuccess(legacyLogin(post("/login"), "storemgr", "warehouse88"), "storemgr");
    }

    @Test
    void f14_seededAccountsUserLogsIn() throws Exception {
        assertLegacySuccess(legacyLogin(post("/login"), "accounts", "ledger2024"), "accounts");
    }

    @Test
    void f15_unauthenticatedTokenIsRejectedInsteadOfMintingAdminToken() throws Exception {
        mockMvc.perform(get("/token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(content().string(not(containsString("tarangparikh"))));
    }

    @Test
    void f16_unknownRouteReturns404ForAuthenticatedCaller() throws Exception {
        mockMvc.perform(get("/nonexistent").with(bearer("tarang"))).andExpect(status().isNotFound());
    }

    @Test
    void everyRowOfTheParitySeedFileLogsIn() throws Exception {
        Path seed = Path.of("..", "parity", "seed", "seed-users.sql");
        Assumptions.assumeTrue(Files.isReadable(seed), "parity seed not available");
        Matcher rows = Pattern.compile("\\('([^']*)',\\s*'([^']*)'\\)").matcher(Files.readString(seed));
        int count = 0;
        while (rows.find()) {
            assertThat(SEED_USERS).containsEntry(rows.group(1), rows.group(2));
            assertLegacySuccess(legacyLogin(post("/login"), rows.group(1), rows.group(2)), rows.group(1));
            count++;
        }
        assertThat(count).isEqualTo(SEED_USERS.size());
    }

    private ResultActions legacyLogin(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String email,
            String password)
            throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", email)
                .param("password", password));
    }

    private void assertLegacySuccess(ResultActions actions, String expectedUsername) throws Exception {
        MvcResult result = actions.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        String jwt = objectMapper.readValue(result.getResponse().getContentAsString(), String.class);
        assertLegacyJwt(jwt, expectedUsername);

        Cookie cookie = result.getResponse().getCookie("token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(jwt);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isBetween(3590, 3600);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Strict");
    }

    private void assertLegacyJwt(String jwt, String expectedUsername) throws Exception {
        assertThat(header(jwt).get("alg").asText()).isEqualTo("RS256");
        JsonNode claims = payload(jwt);
        assertThat(claims.get("sub").asText()).isEqualTo("user/login");
        assertThat(claims.get("username").asText()).isEqualTo(expectedUsername);
        assertThat(claims.get("scope").asText()).isEqualTo("admin");
        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong()).isEqualTo(3600);
        assertThat(jwtService.parse(jwt).get("username", String.class)).isEqualTo(expectedUsername);
    }

    private ResultActions assertLegacyFailure(ResultActions actions) throws Exception {
        return actions.andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/signin?error"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }
}
