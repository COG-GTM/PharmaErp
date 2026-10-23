package com.pharma.erp.auth;

import com.pharma.erp.common.security.JwtService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds the {@code token} cookie: HttpOnly, SameSite=Strict, scoped to the whole app. */
@Component
class TokenCookies {

    private final JwtService jwtService;
    private final AuthProperties properties;

    TokenCookies(JwtService jwtService, AuthProperties properties) {
        this.jwtService = jwtService;
        this.properties = properties;
    }

    ResponseCookie issue(IssuedToken token) {
        Duration maxAge = Duration.between(Instant.now(), token.expiresAt());
        return base(token.token()).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(jwtService.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path("/");
    }
}
