package com.pharma.erp.manufacturers;

import java.math.BigDecimal;
import java.util.List;

/** Stock held from one manufacturer/supplier/distributor, items ordered first-expired-first-out. */
public record ManufacturerStockSummary(
        Integer manufacturerId,
        String manufacturerName,
        ManufacturerType type,
        int itemCount,
        long totalQuantity,
        BigDecimal totalValue,
        List<ManufacturerStockItem> items) {}
