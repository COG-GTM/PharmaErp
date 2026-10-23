package com.pharma.erp.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Logistics status of a stock item (spec sheet "Status": inTransit, arrived, processing, delivered). */
@Entity
@Table(name = "item_status")
public class ItemStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer id;

    @Column(name = "status_name", nullable = false, length = 50)
    private String name;

    protected ItemStatus() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
