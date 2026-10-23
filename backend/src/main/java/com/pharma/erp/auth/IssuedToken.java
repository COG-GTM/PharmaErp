package com.pharma.erp.auth;

import java.time.Instant;

/** A freshly signed JWT and the identity it was issued for. */
public record IssuedToken(String token, String username, String scope, Instant expiresAt) {}
