package com.pharma.erp.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AuthUserRepository extends JpaRepository<AuthUser, String> {

    /**
     * Parameterised lookup that hydrates the username as stored. Unlike {@code findById}, which
     * echoes the requested id back, this lets callers detect case-insensitive collation matches.
     */
    @Query("select u from AuthUser u where u.username = :username")
    Optional<AuthUser> findStoredByUsername(@Param("username") String username);
}
