package com.pharma.erp.auth;

import com.pharma.erp.common.security.JwtService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of the legacy login check: the username is trimmed, then both username and password must
 * match exactly (case-sensitive). Lookups go through JPA by primary key, so user input is never
 * concatenated into a query. Legacy plaintext passwords are upgraded to BCrypt on first success.
 */
@Service
public class AuthService {

    /** The legacy system grants every authenticated user the {@code admin} scope. */
    public static final String LEGACY_SCOPE = "admin";

    private final AuthUserRepository users;
    private final PasswordVerifier passwords;
    private final JwtService jwtService;

    AuthService(AuthUserRepository users, PasswordVerifier passwords, JwtService jwtService) {
        this.users = users;
        this.passwords = passwords;
        this.jwtService = jwtService;
    }

    /** Returns a token when the credentials are valid, empty otherwise. */
    @Transactional
    public Optional<IssuedToken> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            passwords.simulateCheck(password);
            return Optional.empty();
        }
        Optional<AuthUser> found = findExact(trimmed);
        if (found.isEmpty()) {
            passwords.simulateCheck(password);
            return Optional.empty();
        }
        AuthUser user = found.get();
        if (!passwords.matches(password, user.getPassword())) {
            return Optional.empty();
        }
        if (passwords.needsUpgrade(user.getPassword())) {
            user.setPassword(passwords.hash(password));
        }
        return Optional.of(issue(user.getUsername(), LEGACY_SCOPE));
    }

    /**
     * Re-issues a token for an already authenticated user, provided the account still exists and still holds the
     * presented scope. Scopes are never widened or carried over: legacy accounts hold only {@code admin}, so any
     * other scope is refused rather than renewed.
     */
    @Transactional(readOnly = true)
    public Optional<IssuedToken> reissue(String username, String presentedScope) {
        if (username == null || !LEGACY_SCOPE.equals(presentedScope) || findExact(username).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(issue(username, LEGACY_SCOPE));
    }

    IssuedToken issue(String username, String scope) {
        String token = jwtService.issueToken(username, scope);
        return new IssuedToken(
                token, username, scope, jwtService.parse(token).getExpiration().toInstant());
    }

    /** MySQL's default collation is case-insensitive; the legacy check compared usernames exactly. */
    private Optional<AuthUser> findExact(String username) {
        return users.findStoredByUsername(username).filter(u -> u.getUsername().equals(username));
    }
}
