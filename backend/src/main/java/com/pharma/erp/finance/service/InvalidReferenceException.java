package com.pharma.erp.finance.service;

import com.pharma.erp.common.web.ApiExceptionHandler;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** A referenced id (client, store, item, ...) does not exist: 400 problem+json with the shared {@code errors} map. */
public class InvalidReferenceException extends ErrorResponseException {

    private final transient Map<String, List<String>> errors;

    public InvalidReferenceException(Map<String, List<String>> errors) {
        super(HttpStatus.BAD_REQUEST, problem(errors), null);
        this.errors = Map.copyOf(errors);
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }

    private static ProblemDetail problem(Map<String, List<String>> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty(ApiExceptionHandler.ERRORS_PROPERTY, errors);
        return problem;
    }
}
