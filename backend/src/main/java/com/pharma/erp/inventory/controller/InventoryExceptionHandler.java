package com.pharma.erp.inventory.controller;

import com.pharma.erp.common.web.ApiExceptionHandler;
import com.pharma.erp.inventory.service.InvalidReferenceException;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Renders inventory-specific validation failures in the shared 400 problem+json shape. */
@RestControllerAdvice(assignableTypes = InventoryController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class InventoryExceptionHandler {

    @ExceptionHandler(InvalidReferenceException.class)
    ProblemDetail handleInvalidReference(InvalidReferenceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty(ApiExceptionHandler.ERRORS_PROPERTY, Map.of(ex.getField(), List.of(ex.getMessage())));
        return problem;
    }
}
