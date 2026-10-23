package com.pharma.erp.common.security;

/**
 * Optional hook consulted by {@link JwtAuthenticationFilter} after a token's signature and expiry are
 * verified. Returning {@code false} leaves the request anonymous, e.g. for tokens of deleted accounts.
 */
@FunctionalInterface
public interface TokenAccountChecker {

    TokenAccountChecker ALLOW_ALL = username -> true;

    boolean isActive(String username);
}
