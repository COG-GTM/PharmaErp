package com.pharma.erp.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Rendered as 401 problem+json by the shared exception handler. */
class UnauthorizedException extends ErrorResponseException {

    UnauthorizedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail), null);
    }
}
