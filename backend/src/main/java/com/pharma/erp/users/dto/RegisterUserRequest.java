package com.pharma.erp.users.dto;

import com.pharma.erp.users.validation.ValidPassword;
import com.pharma.erp.users.validation.ValidUsername;

/** Registration payload. Both values are trimmed before validation, as in the legacy code. */
public record RegisterUserRequest(@ValidUsername String username, @ValidPassword String password) {

    public RegisterUserRequest {
        username = username == null ? null : username.trim();
        password = password == null ? null : password.trim();
    }

    @Override
    public String toString() {
        return "RegisterUserRequest[username=" + username + ", password=****]";
    }
}
