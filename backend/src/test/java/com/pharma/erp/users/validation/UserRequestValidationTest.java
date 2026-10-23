package com.pharma.erp.users.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharma.erp.users.dto.ChangePasswordRequest;
import com.pharma.erp.users.dto.RegisterUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The Bean Validation constraints on the request records apply the legacy rules after trimming. */
class UserRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static <T> Map<String, String> violations(T bean) {
        Set<ConstraintViolation<T>> result = validator.validate(bean);
        return result.stream().collect(Collectors.toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));
    }

    @Test
    void validRequestHasNoViolations() {
        assertThat(violations(new RegisterUserRequest("pharmacist01", "Secure#2024"))).isEmpty();
    }

    @Test
    void valuesAreTrimmedBeforeValidationLikeLegacy() {
        RegisterUserRequest request = new RegisterUserRequest("  pharmacist01  ", "  parisdkh ");
        assertThat(request.username()).isEqualTo("pharmacist01");
        assertThat(request.password()).isEqualTo("parisdkh");
        assertThat(violations(request)).isEmpty();
    }

    @Test
    void eachLegacyUsernameRuleProducesItsMessage() {
        assertThat(violations(new RegisterUserRequest(null, "parisdkh")))
                .containsEntry("username", "must not be null");
        assertThat(violations(new RegisterUserRequest("john smith", "parisdkh")))
                .containsEntry("username", "must not contain spaces");
        assertThat(violations(new RegisterUserRequest("123456789", "parisdkh")))
                .containsEntry("username", "must not consist only of digits");
        assertThat(violations(new RegisterUserRequest("john-smith", "parisdkh")))
                .containsEntry("username", "may contain only letters, digits and underscores");
        assertThat(violations(new RegisterUserRequest("tarang", "parisdkh")))
                .containsEntry("username", "must be 8 to 12 characters long");
    }

    @Test
    void eachLegacyPasswordRuleProducesItsMessage() {
        assertThat(violations(new RegisterUserRequest("pharmacist01", null)))
                .containsEntry("password", "must not be null");
        assertThat(violations(new RegisterUserRequest("pharmacist01", "pass word1")))
                .containsEntry("password", "must not contain spaces");
        assertThat(violations(new RegisterUserRequest("pharmacist01", "short")))
                .containsEntry("password", "must be at least 8 characters long");
        assertThat(violations(new RegisterUserRequest("pharmacist01", "x".repeat(73))))
                .containsEntry("password", "must be at most 72 bytes long");
    }

    @Test
    void whitespaceOnlyPasswordIsTrimmedToEmptyAndTooShort() {
        assertThat(violations(new RegisterUserRequest("pharmacist01", "          ")))
                .containsEntry("password", "must be at least 8 characters long");
    }

    @Test
    void changePasswordRequestAppliesPasswordRules() {
        assertThat(violations(new ChangePasswordRequest("newpass99"))).isEmpty();
        assertThat(violations(new ChangePasswordRequest("new pass99")))
                .containsEntry("password", "must not contain spaces");
        assertThat(violations(new ChangePasswordRequest("  abc  ")))
                .containsEntry("password", "must be at least 8 characters long");
    }

    @Test
    void toStringNeverContainsThePassword() {
        assertThat(new RegisterUserRequest("pharmacist01", "Secure#2024").toString()).doesNotContain("Secure#2024");
        assertThat(new ChangePasswordRequest("Secure#2024").toString()).doesNotContain("Secure#2024");
    }
}
