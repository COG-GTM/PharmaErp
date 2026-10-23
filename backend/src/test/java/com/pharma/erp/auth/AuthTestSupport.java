package com.pharma.erp.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parity seed users and JWT helpers shared by the auth tests. */
final class AuthTestSupport {

    /** Same rows as {@code parity/seed/seed-users.sql} (legacy plaintext passwords). */
    static final Map<String, String> SEED_USERS = new LinkedHashMap<>();

    static {
        SEED_USERS.put("tarang", "parisdkh");
        SEED_USERS.put("pharmacist01", "Secure#2024");
        SEED_USERS.put("storemgr", "warehouse88");
        SEED_USERS.put("accounts", "ledger2024");
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private AuthTestSupport() {}

    static JsonNode header(String jwt) throws Exception {
        return decode(jwt.split("\\.")[0]);
    }

    static JsonNode payload(String jwt) throws Exception {
        return decode(jwt.split("\\.")[1]);
    }

    private static JsonNode decode(String part) throws Exception {
        return JSON.readTree(new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8));
    }
}
