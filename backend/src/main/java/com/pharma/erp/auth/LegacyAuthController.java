package com.pharma.erp.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Wire-compatible replacements for the legacy servlets.
 *
 * <ul>
 *   <li>{@code GET|POST /login} (form/query {@code email}, {@code password}): success is 200
 *       {@code application/json} whose body is the JWT as a JSON string, plus the {@code token}
 *       cookie; failure redirects to the login page with {@code ?error} (no parameters at all
 *       redirects without it).
 *   <li>{@code GET|POST /token}: re-issues a token for the caller's valid JWT. The legacy servlet
 *       minted an admin token for the hard-coded user {@code tarangparikh} to anyone; this now
 *       returns 401 without a valid token.
 *   <li>{@code GET /} and {@code GET /redirect}: 302 to the SPA login page.
 * </ul>
 */
@RestController
class LegacyAuthController {

    private final AuthService authService;
    private final TokenCookies cookies;
    private final AuthProperties properties;
    private final ObjectMapper objectMapper;

    LegacyAuthController(
            AuthService authService, TokenCookies cookies, AuthProperties properties, ObjectMapper objectMapper) {
        this.authService = authService;
        this.cookies = cookies;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(path = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    ResponseEntity<String> login(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "password", required = false) String password) {
        Optional<IssuedToken> token = authService.login(email, password);
        if (token.isEmpty()) {
            if (email == null && password == null) {
                return redirect(URI.create(properties.loginPageUrl()));
            }
            return redirect(UriComponentsBuilder.fromUriString(properties.loginPageUrl())
                    .queryParam("error")
                    .build()
                    .toUri());
        }
        return tokenResponse(token.get());
    }

    @RequestMapping(path = "/token", method = {RequestMethod.GET, RequestMethod.POST})
    ResponseEntity<String> token() {
        CurrentToken current = CurrentToken.require();
        IssuedToken token = authService
                .reissue(current.username())
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        return tokenResponse(token);
    }

    @RequestMapping(path = {"/", "/redirect"}, method = {RequestMethod.GET, RequestMethod.HEAD})
    ResponseEntity<String> loginPage() {
        return redirect(URI.create(properties.loginPageUrl()));
    }

    private ResponseEntity<String> tokenResponse(IssuedToken token) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.SET_COOKIE, cookies.issue(token).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(toJsonString(token.token()));
    }

    private String toJsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ResponseEntity<String> redirect(URI location) {
        return ResponseEntity.status(302).location(location).build();
    }
}
