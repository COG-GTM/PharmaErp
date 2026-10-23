package com.pharma.erp.inventory.dto;

import com.pharma.erp.inventory.entity.MovementReason;
import java.time.Instant;

public record StockMovementResponse(
        Integer id,
        Integer itemId,
        String status,
        String location,
        Integer quantityChange,
        Integer quantityAfter,
        MovementReason reason,
        String note,
        String performedBy,
        Instant trackedAt) {
}
