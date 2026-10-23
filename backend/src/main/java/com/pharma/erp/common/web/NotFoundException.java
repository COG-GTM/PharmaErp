package com.pharma.erp.common.web;

/** Thrown by services when a requested resource does not exist; mapped to 404 problem+json. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException(resource + " " + id + " not found");
    }
}
