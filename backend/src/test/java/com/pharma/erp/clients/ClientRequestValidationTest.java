package com.pharma.erp.clients;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientRequestValidationTest {

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

    @ParameterizedTest
    @ValueSource(strings = {"+91 98250 11223", "0261-2765432", "(022) 2345 6789", "9825011223", ""})
    void acceptsPhoneFormats(String phone) {
        assertThat(violatedFields(request("Client", phone, null))).doesNotContain("phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"call me", "12345", "+91 98250 11223 ext 4", "123456789012345678901"})
    void rejectsInvalidPhones(String phone) {
        assertThat(violatedFields(request("Client", phone, null))).contains("phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"27AAPFU0939F1ZV", "24aaacs1234f1z5", ""})
    void acceptsGstin(String gstin) {
        assertThat(violatedFields(request("Client", null, gstin))).doesNotContain("gstin");
    }

    @ParameterizedTest
    @ValueSource(strings = {"27AAPFU0939F1Z", "27AAPFU0939F1AV", "XXAAPFU0939F1ZV"})
    void rejectsInvalidGstin(String gstin) {
        assertThat(violatedFields(request("Client", null, gstin))).contains("gstin");
    }

    @Test
    void requiresName() {
        assertThat(violatedFields(request(" ", null, null))).containsExactly("name");
        assertThat(violatedFields(request(null, null, null))).containsExactly("name");
    }

    @Test
    void feedbackRatingMustBeBetweenOneAndFive() {
        assertThat(validator.validate(new FeedbackRequest(1, "ok", null))).isEmpty();
        assertThat(validator.validate(new FeedbackRequest(5, "ok", 3))).isEmpty();
        assertThat(validator.validate(new FeedbackRequest(0, "ok", null))).hasSize(1);
        assertThat(validator.validate(new FeedbackRequest(6, "ok", null))).hasSize(1);
        assertThat(validator.validate(new FeedbackRequest(3, "ok", 0))).hasSize(1);
    }

    @Test
    void digitsOnlyStripsFormatting() {
        assertThat(ClientSpecifications.digitsOnly("+91 (982) 501-1223")).isEqualTo("919825011223");
        assertThat(ClientSpecifications.digitsOnly(null)).isEmpty();
    }

    private static ClientRequest request(String name, String phone, String gstin) {
        return new ClientRequest(name, null, phone, null, null, null, null, gstin, null);
    }

    private Set<String> violatedFields(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
