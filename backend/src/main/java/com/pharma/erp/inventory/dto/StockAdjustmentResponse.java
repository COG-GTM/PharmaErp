package com.pharma.erp.inventory.dto;

public record StockAdjustmentResponse(InventoryItemResponse item, StockMovementResponse movement) {
}
