package com.pharma.erp.manufacturers;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read-only view of an inventory item sourced from a manufacturer (use case "Stock (Manufacturer)"). */
public record ManufacturerStockItem(
        Integer itemId,
        String itemName,
        String batchNo,
        LocalDate expiryDate,
        int quantity,
        BigDecimal unitPrice,
        Integer storeId,
        String status) {}
