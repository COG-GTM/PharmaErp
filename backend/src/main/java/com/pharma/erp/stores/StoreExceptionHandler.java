package com.pharma.erp.stores;

import com.pharma.erp.common.web.ApiExceptionHandler;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Store-specific errors, rendered in the same problem+json shape as {@link ApiExceptionHandler}. */
@RestControllerAdvice(assignableTypes = StoreController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class StoreExceptionHandler {

    @ExceptionHandler(InvalidReferenceException.class)
    ProblemDetail handleInvalidReference(InvalidReferenceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty(ApiExceptionHandler.ERRORS_PROPERTY, Map.of(ex.getField(), List.of(ex.getMessage())));
        return problem;
    }
}
