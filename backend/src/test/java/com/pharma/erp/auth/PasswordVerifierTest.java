package com.pharma.erp.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordVerifierTest {

    private final PasswordVerifier verifier = new PasswordVerifier(new BCryptPasswordEncoder(4));

    @Test
    void legacyPlaintextMatchesExactlyAndNeedsUpgrade() {
        assertThat(verifier.matches("parisdkh", "parisdkh")).isTrue();
        assertThat(verifier.matches("PARISDKH", "parisdkh")).isFalse();
        assertThat(verifier.matches("parisdkh ", "parisdkh")).isFalse();
        assertThat(verifier.needsUpgrade("parisdkh")).isTrue();
    }

    @Test
    void bcryptHashesAreVerifiedAndNotUpgraded() {
        String hash = verifier.hash("Secure#2024");
        assertThat(PasswordVerifier.isHashed(hash)).isTrue();
        assertThat(verifier.needsUpgrade(hash)).isFalse();
        assertThat(verifier.matches("Secure#2024", hash)).isTrue();
        assertThat(verifier.matches("secure#2024", hash)).isFalse();
        assertThat(verifier.matches(hash, hash)).isFalse();
    }

    @Test
    void delegatingPrefixAndOtherBcryptVariantsAreRecognised() {
        String hash = verifier.hash("ledger2024");
        assertThat(verifier.matches("ledger2024", "{bcrypt}" + hash)).isTrue();
        assertThat(PasswordVerifier.isHashed(hash.replaceFirst("^\\$2a", "\\$2y"))).isTrue();
        assertThat(PasswordVerifier.isHashed("$2a$10$tooShort")).isFalse();
    }

    @Test
    void nullsNeverMatch() {
        assertThat(verifier.matches(null, "x")).isFalse();
        assertThat(verifier.matches("x", null)).isFalse();
    }
}
