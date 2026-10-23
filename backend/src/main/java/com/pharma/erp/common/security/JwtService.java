package com.pharma.erp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies RS256 JWTs. Tokens keep the legacy shape: {@code sub = "user/login"} with
 * {@code username} and {@code scope} claims.
 */
@Service
public class JwtService {

    public static final String LEGACY_SUBJECT = "user/login";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_SCOPE = "scope";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties properties;
    private final Clock clock;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        KeyPair keyPair = loadOrGenerate(properties.privateKeyPath(), properties.publicKeyPath());
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    /** Issues a token in the legacy format for {@code username} with a space-separated {@code scope}. */
    public String issueToken(String username, String scope) {
        return issueToken(LEGACY_SUBJECT, Map.of(CLAIM_USERNAME, username, CLAIM_SCOPE, scope));
    }

    public String issueToken(String subject, Map<String, ?> claims) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.ttl())))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Verifies signature, algorithm and expiry.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    public String cookieName() {
        return properties.cookieName();
    }

    static KeyPair loadOrGenerate(Path privatePath, Path publicPath) {
        if (privatePath != null
                && publicPath != null
                && Files.isReadable(privatePath)
                && Files.isReadable(publicPath)) {
            log.info("Loading JWT RSA key pair from {} / {}", privatePath, publicPath);
            return new KeyPair(readPublicKey(publicPath), readPrivateKey(privatePath));
        }
        log.warn("JWT key paths not configured or unreadable; generating an ephemeral RSA key pair");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot generate RSA key pair", e);
        }
    }

    static RSAPrivateKey readPrivateKey(Path path) {
        try {
            byte[] der = decodePem(Files.readString(path), "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid RSA private key in " + path, e);
        }
    }

    static RSAPublicKey readPublicKey(Path path) {
        try {
            byte[] der = decodePem(Files.readString(path), "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid RSA public key in " + path, e);
        }
    }

    private static byte[] decodePem(String pem, String type) {
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < start) {
            throw new IllegalArgumentException("Expected PEM block '" + type + "'");
        }
        String body = pem.substring(start + begin.length(), stop).replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
