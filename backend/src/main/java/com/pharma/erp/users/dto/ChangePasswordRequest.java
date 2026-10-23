package com.pharma.erp.users.dto;

import com.pharma.erp.users.validation.ValidPassword;

/** New password for an existing user; trimmed before validation, as in the legacy code. */
public record ChangePasswordRequest(@ValidPassword String password) {

    public ChangePasswordRequest {
        password = password == null ? null : password.trim();
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest[password=****]";
    }
}
