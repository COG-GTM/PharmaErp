package com.pharma.erp.auth;

import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** The identity established by {@code JwtAuthenticationFilter} for the current request. */
record CurrentToken(String username, String scope, String token) {

    private static final String SCOPE_PREFIX = "SCOPE_";

    /** @throws UnauthorizedException when the request carries no valid JWT */
    static CurrentToken require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof UsernamePasswordAuthenticationToken)
                || !auth.isAuthenticated()
                || auth.getName() == null
                || !(auth.getCredentials() instanceof String token)) {
            throw new UnauthorizedException("Authentication required");
        }
        String scope = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(SCOPE_PREFIX))
                .map(a -> a.substring(SCOPE_PREFIX.length()))
                .collect(Collectors.joining(" "));
        return new CurrentToken(auth.getName(), scope, token);
    }
}
