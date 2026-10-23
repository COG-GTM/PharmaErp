package com.pharma.erp.staff;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharma.erp.staff.dto.StaffRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StaffRequestValidationTest {

    private static jakarta.validation.ValidatorFactory factory;
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

    private static StaffRequest withUsername(String username) {
        return new StaffRequest("Asha", null, null, LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), null, null,
                null, null, null, null, null, null, null, null, null, null, null, username, null, null, null);
    }

    private Set<String> invalidFields(StaffRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tarangparikh", "user_001", "abcdefgh", "a1234567"})
    void legacyUsernameRuleAcceptsValidNames(String username) {
        assertThat(invalidFields(withUsername(username))).isEmpty();
    }

    @Test
    void usernameEmailAndMobileAreTrimmedBeforeValidation() {
        StaffRequest padded = new StaffRequest("Asha", null, null, null, null, null, null, null, null, null, null,
                " 9876543210 ", " asha@example.com ", null, null, null, null, null, "  tarangparikh  ", null, null,
                null);
        assertThat(invalidFields(padded)).isEmpty();
        assertThat(padded.username()).isEqualTo("tarangparikh");
        assertThat(padded.email()).isEqualTo("asha@example.com");
        assertThat(padded.mobileNumber()).isEqualTo("9876543210");
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "12345678", "has space1", "way_too_long_name", "bad-char1"})
    void legacyUsernameRuleRejectsInvalidNames(String username) {
        assertThat(invalidFields(withUsername(username))).containsExactly("username");
    }
}
