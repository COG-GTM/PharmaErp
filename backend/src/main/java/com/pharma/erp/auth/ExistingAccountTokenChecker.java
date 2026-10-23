package com.pharma.erp.auth;

import com.pharma.erp.common.security.TokenAccountChecker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rejects otherwise valid JWTs whose {@code username} no longer exists in the {@code user} table, so
 * deleting an account revokes its outstanding tokens immediately instead of at expiry.
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
    public boolean isActive(String username) {
        return username != null && users.existsById(username);
    }
}
