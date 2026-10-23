package com.pharma.erp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Verifies passwords stored either as BCrypt hashes or, for rows inherited from the legacy system,
 * as plaintext. Plaintext rows are compared in constant time and flagged for upgrade to BCrypt.
 */
@Component
class PasswordVerifier {

    private static final Pattern BCRYPT = Pattern.compile("^(\\{bcrypt})?\\$2[aby]?\\$\\d\\d\\$[./0-9A-Za-z]{53}$");
    private static final String BCRYPT_PREFIX = "{bcrypt}";

    private final BCryptPasswordEncoder encoder;
    private final String dummyHash;

    PasswordVerifier() {
        this(new BCryptPasswordEncoder());
    }

    PasswordVerifier(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode("pharma-erp-timing-equaliser");
    }

    boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isHashed(storedPassword)) {
            return encoder.matches(rawPassword, stripPrefix(storedPassword));
        }
        return MessageDigest.isEqual(
                rawPassword.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8));
    }

    boolean needsUpgrade(String storedPassword) {
        return !isHashed(storedPassword);
    }

    String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /** Burns roughly one BCrypt verification so unknown usernames are not distinguishable by timing. */
    void simulateCheck(String rawPassword) {
        encoder.matches(rawPassword == null ? "" : rawPassword, dummyHash);
    }

    static boolean isHashed(String storedPassword) {
        return storedPassword != null && BCRYPT.matcher(storedPassword).matches();
    }

    private static String stripPrefix(String stored) {
        return stored.startsWith(BCRYPT_PREFIX) ? stored.substring(BCRYPT_PREFIX.length()) : stored;
    }
}
