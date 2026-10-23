package com.pharma.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static JwtProperties ephemeral() {
        return new JwtProperties(null, null, Duration.ofHours(1), "token");
    }

    @Test
    void issuesLegacyShapedRs256Token() {
        JwtService service = new JwtService(ephemeral(), Clock.fixed(NOW, ZoneOffset.UTC));

        String token = service.issueToken("tarang", "admin");
        Claims claims = service.parse(token);

        assertThat(claims.getSubject()).isEqualTo("user/login");
        assertThat(claims.get("username", String.class)).isEqualTo("tarang");
        assertThat(claims.get("scope", String.class)).isEqualTo("admin");
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        String header = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertThat(header).contains("\"alg\":\"RS256\"");
    }

    @Test
    void rejectsExpiredToken() {
        MutableClock clock = new MutableClock(NOW);
        JwtService service = new JwtService(ephemeral(), clock);
        String token = service.issueToken("tarang", "admin");

        clock.instant = NOW.plus(Duration.ofHours(1)).plusSeconds(1);

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTokenSignedByAnotherKey() {
        String foreign = new JwtService(ephemeral()).issueToken("tarang", "admin");
        JwtService service = new JwtService(ephemeral());

        assertThatThrownBy(() -> service.parse(foreign)).isInstanceOf(SignatureException.class);
    }

    @Test
    void loadsPemKeyPairFromConfiguredPaths(@TempDir Path dir) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        Path privatePem = dir.resolve("private.pem");
        Path publicPem = dir.resolve("public.pem");
        Files.writeString(privatePem, pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        Files.writeString(publicPem, pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        JwtProperties props = new JwtProperties(privatePem, publicPem, Duration.ofMinutes(5), "token");

        JwtService first = new JwtService(props);
        JwtService second = new JwtService(props);

        assertThat(first.publicKey()).isEqualTo(pair.getPublic());
        assertThat(second.parse(first.issueToken("user/login", Map.of("username", "a"))).get("username"))
                .isEqualTo("a");
    }

    @Test
    void generatesEphemeralKeysWhenPathsMissing(@TempDir Path dir) {
        JwtProperties props =
                new JwtProperties(dir.resolve("missing.pem"), dir.resolve("missing.pub"), Duration.ofHours(1), "token");

        JwtService service = new JwtService(props);

        assertThat(service.parse(service.issueToken("tarang", "admin")).get("username")).isEqualTo("tarang");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private static String pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }
}
