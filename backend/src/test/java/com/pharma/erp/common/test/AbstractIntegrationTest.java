package com.pharma.erp.common.test;

import com.pharma.erp.common.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests: full Spring context + MockMvc against a real MySQL 8 started
 * once per JVM by Testcontainers (Flyway migrations applied on context start). Subclasses share
 * the database, so tests must create their own data and must not depend on row counts or IDs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0")).withDatabaseName("PHARMA_ERP");

    static {
        MYSQL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    /** Legacy-format token for {@code username} with scope {@code admin}. */
    protected String tokenFor(String username) {
        return jwtService.issueToken(username, "admin");
    }

    /** MockMvc post-processor adding {@code Authorization: Bearer <token>} for {@code username}. */
    protected RequestPostProcessor bearer(String username) {
        String token = tokenFor(username);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
