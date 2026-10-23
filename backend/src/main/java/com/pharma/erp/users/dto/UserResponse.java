package com.pharma.erp.users.dto;

import com.pharma.erp.users.entity.UserAccount;

public record UserResponse(String username) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getUsername());
    }
}
