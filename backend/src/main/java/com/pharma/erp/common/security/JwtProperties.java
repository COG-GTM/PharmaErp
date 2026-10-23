package com.pharma.erp.common.security;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * RS256 key material and token settings. When either PEM path is unset or unreadable an ephemeral
 * key pair is generated at startup (tokens then do not survive a restart).
 *
 * @param privateKeyPath PKCS#8 PEM ("BEGIN PRIVATE KEY")
 * @param publicKeyPath X.509 PEM ("BEGIN PUBLIC KEY")
 * @param ttl token lifetime (legacy: 1 hour)
 * @param cookieName cookie the legacy client stores the token in
 */
@ConfigurationProperties(prefix = "pharma.security.jwt")
public record JwtProperties(
        Path privateKeyPath,
        Path publicKeyPath,
        @DefaultValue("PT1H") Duration ttl,
        @DefaultValue("token") String cookieName) {}
