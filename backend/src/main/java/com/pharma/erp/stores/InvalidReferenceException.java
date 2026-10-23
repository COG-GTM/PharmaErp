package com.pharma.erp.stores;

/** A request field or parameter refers to something that does not exist; rendered as a 400 field error. */
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
