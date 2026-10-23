package com.pharma.erp.common.security;

import java.time.Instant;

/**
 * Optional hook consulted by {@link JwtAuthenticationFilter} after a token's signature and expiry are
 * verified. Returning {@code false} leaves the request anonymous, e.g. for tokens of deleted accounts or
 * tokens issued before the account was (re)created.
 */
@FunctionalInterface
public interface TokenAccountChecker {

    TokenAccountChecker ALLOW_ALL = (username, issuedAt) -> true;

    boolean isActive(String username, Instant issuedAt);
}
