package com.pharma.erp.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginTypeRequest(@NotBlank @Size(max = 100) String name) {
}
