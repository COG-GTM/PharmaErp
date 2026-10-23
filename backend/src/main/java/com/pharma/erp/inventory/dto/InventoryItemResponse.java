package com.pharma.erp.inventory.dto;

import com.pharma.erp.inventory.entity.StockStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An inventory batch. {@code status} is the logistics status (IN_TRANSIT, ARRIVED, ...); {@code stockStatus}
 * is derived from quantity, reorder level and expiry; {@code daysToExpiry} is negative once expired.
 */
public record InventoryItemResponse(
        Integer id,
        String name,
        String description,
        String batchNo,
        LocalDate expiryDate,
        Long daysToExpiry,
        int quantity,
        int reorderLevel,
        BigDecimal unitPrice,
        BigDecimal stockValue,
        BigDecimal gstRate,
        String hsnCode,
        String barcode,
        Integer manufacturerId,
        Integer storeId,
        String status,
        StockStatus stockStatus,
        Integer employeeId,
        Integer clientId,
        LocalDate assignDate,
        LocalDate deliveryDate,
        BigDecimal totalAmount,
        BigDecimal advanceAmount,
        Instant createdAt,
        Instant updatedAt) {
}
