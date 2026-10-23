package com.pharma.erp.inventory.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Body of {@code POST /api/inventory}: a new batch, with its opening quantity recorded as a RECEIPT movement. */
public record InventoryItemCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]*$", message = "must contain only letters, digits, '.', '_', '/' or '-'") String batchNo,
        LocalDate expiryDate,
        @NotNull @PositiveOrZero Integer quantity,
        @PositiveOrZero Integer reorderLevel,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal gstRate,
        @Pattern(regexp = "^[0-9]{4,8}$", message = "must be 4 to 8 digits") String hsnCode,
        @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "must contain only letters, digits or '-'") String barcode,
        @Positive Integer manufacturerId,
        @Positive Integer storeId,
        @Size(max = 50) String status,
        @Positive Integer employeeId,
        @Positive Integer clientId,
        LocalDate assignDate,
        LocalDate deliveryDate,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal totalAmount,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal advanceAmount,
        @Size(max = 255) String location) implements ItemDetails {
}
