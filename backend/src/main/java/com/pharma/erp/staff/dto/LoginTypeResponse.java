package com.pharma.erp.staff.dto;

import com.pharma.erp.staff.entity.LoginType;

public record LoginTypeResponse(Integer id, String name) {

    public static LoginTypeResponse from(LoginType loginType) {
        return new LoginTypeResponse(loginType.getId(), loginType.getName());
    }
}
