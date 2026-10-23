package com.pharma.erp.common.web;

/** Thrown when a request conflicts with current state (duplicate key, invalid transition); mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
