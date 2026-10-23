package com.pharma.erp.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharma.erp.common.test.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "pharma.auth.reject-deleted-accounts=true")
class DeletedAccountTokenIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void tokenStopsWorkingOnceTheAccountIsDeleted() throws Exception {
        String username = "revoke_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO `user` (`username`, `password`) VALUES (?, ?)", username, "Secret#123");
        String token = tokenFor(username);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        jdbc.update("DELETE FROM `user` WHERE `username` = ?", username);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedAccountStaysRevokedWhenTheUsernameIsReRegistered() throws Exception {
        String username = "reuse_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO `user` (`username`, `password`) VALUES (?, ?)", username, "Secret#123");
        String oldToken = tokenFor(username);
        jdbc.update("DELETE FROM `user` WHERE `username` = ?", username);
        Thread.sleep(1_100); // JWT iat and created_at have one-second resolution
        jdbc.update("INSERT INTO `user` (`username`, `password`) VALUES (?, ?)", username.toUpperCase(), "Other#456");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenFor(username)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenForUnknownAccountIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(bearer("never_existed_" + UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }
}
