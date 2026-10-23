package com.pharma.erp.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 255) String username,
        @NotNull @Size(min = 1, max = 255) String password) {}
