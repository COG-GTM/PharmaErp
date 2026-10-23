package com.pharma.erp.auth;

import com.pharma.erp.common.security.TokenAccountChecker;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rejects otherwise valid JWTs whose {@code username} no longer exists in the {@code user} table, or whose
 * account was created after the token was issued. Deleting an account therefore revokes its outstanding
 * tokens immediately, and re-registering the same (or a case-variant) username does not revive them.
 * Disable with {@code pharma.auth.reject-deleted-accounts=false}.
 */
@Component
@ConditionalOnProperty(name = "pharma.auth.reject-deleted-accounts", havingValue = "true", matchIfMissing = true)
class ExistingAccountTokenChecker implements TokenAccountChecker {

    private final AuthUserRepository users;

    ExistingAccountTokenChecker(AuthUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String username, Instant issuedAt) {
        return username != null && issuedAt != null
                && users.countCreatedAtOrBefore(username, issuedAt.getEpochSecond()) > 0;
    }
}
