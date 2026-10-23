package com.pharma.erp.auth;

import java.time.Instant;

public record LoginResponse(String token, String username, String scope, Instant expiresAt) {

    static LoginResponse from(IssuedToken token) {
        return new LoginResponse(token.token(), token.username(), token.scope(), token.expiresAt());
    }
}
