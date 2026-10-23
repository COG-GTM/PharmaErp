package com.pharma.erp.inventory.dto;

import com.pharma.erp.inventory.entity.MovementReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/inventory/{id}/adjust}: a signed quantity change with its reason. Inbound
 * reasons (RECEIPT, RETURN, TRANSFER_IN) must be positive, outbound ones (SALE, TRANSFER_OUT, DAMAGE,
 * EXPIRY_WRITE_OFF) negative; CORRECTION may be either.
 */
public record StockAdjustmentRequest(
        @NotNull Integer quantityChange,
        @NotNull MovementReason reason,
        @Size(max = 500) String note,
        @Size(max = 255) String location) {
}
