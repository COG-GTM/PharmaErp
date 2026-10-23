package com.pharma.erp.users.validation;

import java.nio.charset.StandardCharsets;

/**
 * Port of the legacy {@code com.pharma.erp.Validation.UserValidation} username/password rules.
 * Rules are evaluated in the legacy order and the first failure wins. Callers pass values that
 * have already been trimmed, as the legacy code trimmed before validating.
 */
public final class LegacyUserRules {

    /** BCrypt only uses the first 72 bytes of a password; longer values are rejected (not a legacy rule). */
    public static final int BCRYPT_MAX_BYTES = 72;

    public enum UsernameError {
        NULL_VALUE("must not be null"),
        CONTAINS_SPACE("must not contain spaces"),
        ONLY_NUMBER("must not consist only of digits"),
        INVALID_CHARACTER("may contain only letters, digits and underscores"),
        LENGTH_ERROR("must be 8 to 12 characters long");

        private final String message;

        UsernameError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    public enum PasswordError {
        NULL_VALUE("must not be null"),
        CONTAINS_SPACE("must not contain spaces"),
        LENGTH_ERROR("must be at least 8 characters long"),
        TOO_LONG("must be at most " + BCRYPT_MAX_BYTES + " bytes long");

        private final String message;

        PasswordError(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    private LegacyUserRules() {}

    /** Returns the first violated username rule, or {@code null} when the username is valid. */
    public static UsernameError checkUsername(String username) {
        if (username == null) {
            return UsernameError.NULL_VALUE;
        }
        if (username.contains(" ")) {
            return UsernameError.CONTAINS_SPACE;
        }
        if (username.matches("(\\d)+")) {
            return UsernameError.ONLY_NUMBER;
        }
        if (!username.matches("(\\w|\\d)+")) {
            return UsernameError.INVALID_CHARACTER;
        }
        if (!username.matches("(\\w|\\d){8,12}")) {
            return UsernameError.LENGTH_ERROR;
        }
        return null;
    }

    /** Returns the first violated password rule, or {@code null} when the password is valid. */
    public static PasswordError checkPassword(String password) {
        if (password == null) {
            return PasswordError.NULL_VALUE;
        }
        if (password.contains(" ")) {
            return PasswordError.CONTAINS_SPACE;
        }
        if (!password.matches("(.{8,})")) {
            return PasswordError.LENGTH_ERROR;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            return PasswordError.TOO_LONG;
        }
        return null;
    }
}
