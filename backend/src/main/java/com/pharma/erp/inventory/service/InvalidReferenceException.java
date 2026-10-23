package com.pharma.erp.inventory.service;

/** A request body refers to a row of another module (store, manufacturer, ...) that does not exist. */
public class InvalidReferenceException extends RuntimeException {

    private final String field;

    public InvalidReferenceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
