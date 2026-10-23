package com.pharma.erp.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharma.erp.common.test.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class ApplicationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void contextLoadsAndBaselineSchemaIsMigrated() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);
        assertThat(tables).contains(
                "user", "student", "login_type", "staff", "staff_security", "salary", "store", "client",
                "manufacturer", "item_status", "inventory_item", "stock_tracking", "prescription",
                "prescription_line", "invoice", "invoice_line", "expense", "payment_method", "payment",
                "feedback", "flyway_schema_history");
    }

    @Test
    void legacySeedLoadsIntoUserTable() {
        jdbc.update("INSERT INTO `user` (`username`, `password`) VALUES ('scaffold_it', 'parisdkh')");
        assertThat(jdbc.queryForObject("SELECT password FROM `user` WHERE username = 'scaffold_it'", String.class))
                .isEqualTo("parisdkh");
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/anything").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerTokenAuthenticates() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(bearer("tarang")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void tokenCookieAuthenticates() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").cookie(new Cookie("token", tokenFor("tarang"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
