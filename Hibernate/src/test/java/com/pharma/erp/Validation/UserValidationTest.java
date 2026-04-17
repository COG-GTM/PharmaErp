package com.pharma.erp.Validation;

import com.schema.User;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for UserValidation (validation logic only, no DB required).
 */
public class UserValidationTest {

    // We test usernameValidation and passwordValidation which don't require a DB session
    // We use a helper to create a UserValidation with null session (only for validation methods)

    private UserValidation createValidator() {
        return new UserValidation(null, null);
    }

    // --- Username Validation Tests ---

    @Test
    public void testUsernameValidation_validUsername() {
        UserValidation validator = createValidator();
        User user = new User("validuser", "password");
        assertEquals(UserValidation.TypeError.NO_ERROR, validator.usernameValidation(user));
    }

    @Test
    public void testUsernameValidation_tooShort() {
        UserValidation validator = createValidator();
        User user = new User("short", "password");
        assertEquals(UserValidation.TypeError.LENGTH_ERROR, validator.usernameValidation(user));
    }

    @Test
    public void testUsernameValidation_tooLong() {
        UserValidation validator = createValidator();
        User user = new User("thisusernameistoolong", "password");
        assertEquals(UserValidation.TypeError.LENGTH_ERROR, validator.usernameValidation(user));
    }

    @Test
    public void testUsernameValidation_containsSpace() {
        UserValidation validator = createValidator();
        User user = new User("user name", "password");
        assertEquals(UserValidation.TypeError.CONTAINS_SPACE, validator.usernameValidation(user));
    }

    @Test
    public void testUsernameValidation_onlyNumbers() {
        UserValidation validator = createValidator();
        User user = new User("12345678", "password");
        assertEquals(UserValidation.TypeError.ONLY_NUMBER, validator.usernameValidation(user));
    }

    @Test
    public void testUsernameValidation_invalidCharacters() {
        UserValidation validator = createValidator();
        User user = new User("user@name", "password");
        assertEquals(UserValidation.TypeError.INVALID_CHARACTER, validator.usernameValidation(user));
    }

    @Test
    public void testUsernameValidation_nullUsername() {
        UserValidation validator = createValidator();
        User user = new User(null, "password");
        // trimData will throw NPE on null, which is expected behavior
        try {
            validator.usernameValidation(user);
            fail("Expected NullPointerException for null username");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // --- Password Validation Tests ---

    @Test
    public void testPasswordValidation_validPassword() {
        UserValidation validator = createValidator();
        User user = new User("username", "validpass");
        assertEquals(UserValidation.PassWordTypeError.VALID, validator.passwordValidation(user));
    }

    @Test
    public void testPasswordValidation_tooShort() {
        UserValidation validator = createValidator();
        User user = new User("username", "short");
        assertEquals(UserValidation.PassWordTypeError.LENGTH_ERROR, validator.passwordValidation(user));
    }

    @Test
    public void testPasswordValidation_containsSpace() {
        UserValidation validator = createValidator();
        User user = new User("username", "pass word");
        assertEquals(UserValidation.PassWordTypeError.CONTAINS_SPACE, validator.passwordValidation(user));
    }

    @Test
    public void testPasswordValidation_nullPassword() {
        UserValidation validator = createValidator();
        User user = new User("username", null);
        try {
            validator.passwordValidation(user);
            fail("Expected NullPointerException for null password");
        } catch (NullPointerException e) {
            // Expected because trimData trims before null check
        }
    }

    // --- isValid Tests ---

    @Test
    public void testIsValid_validCredentials() {
        UserValidation validator = createValidator();
        User user = new User("validuser", "validpassword");
        assertTrue(validator.isValid(user));
    }

    @Test
    public void testIsValid_invalidUsername() {
        UserValidation validator = createValidator();
        User user = new User("bad", "validpassword");
        assertFalse(validator.isValid(user));
    }

    @Test
    public void testIsValid_invalidPassword() {
        UserValidation validator = createValidator();
        User user = new User("validuser", "short");
        assertFalse(validator.isValid(user));
    }

    @Test
    public void testIsValid_bothInvalid() {
        UserValidation validator = createValidator();
        User user = new User("bad", "short");
        assertFalse(validator.isValid(user));
    }
}
