package com.pharma.erp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying a JWT in {@code Authorization: Bearer} or the {@code token} cookie.
 * The principal is the {@code username} claim; each entry of the space-separated {@code scope} claim
 * becomes a {@code SCOPE_<value>} authority. Invalid tokens are ignored (request stays anonymous).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token);
                String username = claims.get(JwtService.CLAIM_USERNAME, String.class);
                if (username == null) {
                    username = claims.getSubject();
                }
                var authentication = new UsernamePasswordAuthenticationToken(username, token, authorities(claims));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                logger.debug("Ignoring invalid JWT: " + e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String value = header.substring(BEARER_PREFIX.length()).trim();
            return value.isEmpty() ? null : value;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (jwtService.cookieName().equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private static List<SimpleGrantedAuthority> authorities(Claims claims) {
        Object scope = claims.get(JwtService.CLAIM_SCOPE);
        if (!(scope instanceof String scopes) || scopes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(scopes.trim().split("\\s+"))
                .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                .toList();
    }
}
