package com.pharma.erp.inventory.entity;

import java.time.LocalDate;

/**
 * Stock level of an item, derived on read (never stored). Precedence: an expired batch is
 * {@link #EXPIRED} whatever its quantity; otherwise zero is {@link #OUT_OF_STOCK}, at or below the
 * reorder level is {@link #LOW}, else {@link #IN_STOCK}. A batch is usable through its expiry date.
 */
public enum StockStatus {
    IN_STOCK,
    LOW,
    OUT_OF_STOCK,
    EXPIRED;

    public static StockStatus derive(int quantity, LocalDate expiryDate, int reorderLevel, LocalDate today) {
        if (expiryDate != null && expiryDate.isBefore(today)) {
            return EXPIRED;
        }
        if (quantity <= 0) {
            return OUT_OF_STOCK;
        }
        if (quantity <= reorderLevel) {
            return LOW;
        }
        return IN_STOCK;
    }
}
