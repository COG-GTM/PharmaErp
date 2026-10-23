package com.pharma.erp.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Editable item attributes shared by the create and update requests (quantity only moves via adjustments). */
public interface ItemDetails {

    String name();

    String description();

    String batchNo();

    LocalDate expiryDate();

    Integer reorderLevel();

    BigDecimal unitPrice();

    BigDecimal gstRate();

    String hsnCode();

    String barcode();

    Integer manufacturerId();

    Integer storeId();

    String status();

    Integer employeeId();

    Integer clientId();

    LocalDate assignDate();

    LocalDate deliveryDate();

    BigDecimal totalAmount();

    BigDecimal advanceAmount();
}
