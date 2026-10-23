package com.pharma.erp.users.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharma.erp.users.validation.LegacyUserRules.PasswordError;
import com.pharma.erp.users.validation.LegacyUserRules.UsernameError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Mirrors each branch of legacy {@code UserValidation.usernameValidation/passwordValidation}. */
class LegacyUserRulesTest {

    @Test
    void usernameNullIsNullValue() {
        assertThat(LegacyUserRules.checkUsername(null)).isEqualTo(UsernameError.NULL_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"john smith", "a b", "pharma user1", " "})
    void usernameWithSpaceIsContainsSpace(String username) {
        assertThat(LegacyUserRules.checkUsername(username)).isEqualTo(UsernameError.CONTAINS_SPACE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "1", "123456789012345"})
    void usernameOfOnlyDigitsIsOnlyNumber(String username) {
        assertThat(LegacyUserRules.checkUsername(username)).isEqualTo(UsernameError.ONLY_NUMBER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pharma-user", "user.name1", "o'connor12", "x'", "", "tab\tuser1", "usér12345"})
    void usernameWithNonWordCharacterIsInvalidCharacter(String username) {
        assertThat(LegacyUserRules.checkUsername(username)).isEqualTo(UsernameError.INVALID_CHARACTER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"tarang", "abc1234", "a", "abcdefghijklm", "pharmacist0123"})
    void usernameOutside8To12IsLengthError(String username) {
        assertThat(LegacyUserRules.checkUsername(username)).isEqualTo(UsernameError.LENGTH_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "abcdefghijkl", "pharmacist01", "store_mgr_1", "12345678a", "________"})
    void validUsernames(String username) {
        assertThat(LegacyUserRules.checkUsername(username)).isNull();
    }

    @Test
    void spaceCheckWinsOverDigitAndLengthChecks() {
        assertThat(LegacyUserRules.checkUsername("1234 5678")).isEqualTo(UsernameError.CONTAINS_SPACE);
    }

    @Test
    void onlyNumberWinsOverLengthCheck() {
        assertThat(LegacyUserRules.checkUsername("123")).isEqualTo(UsernameError.ONLY_NUMBER);
    }

    @Test
    void passwordNullIsNullValue() {
        assertThat(LegacyUserRules.checkPassword(null)).isEqualTo(PasswordError.NULL_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pass word1", "a b", "longpassword with space"})
    void passwordWithSpaceIsContainsSpace(String password) {
        assertThat(LegacyUserRules.checkPassword(password)).isEqualTo(PasswordError.CONTAINS_SPACE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "1234567", "short#1"})
    void passwordShorterThan8IsLengthError(String password) {
        assertThat(LegacyUserRules.checkPassword(password)).isEqualTo(PasswordError.LENGTH_ERROR);
    }

    @Test
    void passwordWithLineBreakFailsLegacyDotPattern() {
        assertThat(LegacyUserRules.checkPassword("abcd\nefgh")).isEqualTo(PasswordError.LENGTH_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"parisdkh", "Secure#2024", "warehouse88", "12345678", "!@#$%^&*"})
    void validPasswords(String password) {
        assertThat(LegacyUserRules.checkPassword(password)).isNull();
    }

    @Test
    void passwordAtBcryptLimitIsValidAndAboveIsTooLong() {
        assertThat(LegacyUserRules.checkPassword("a".repeat(72))).isNull();
        assertThat(LegacyUserRules.checkPassword("a".repeat(73))).isEqualTo(PasswordError.TOO_LONG);
        assertThat(LegacyUserRules.checkPassword("é".repeat(37))).isEqualTo(PasswordError.TOO_LONG);
    }
}
