package com.pharma.erp.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One row of the stock tracking history (spec sheet "Tracking"): a quantity movement or status change. */
@Entity
@Table(name = "stock_tracking")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracking_id")
    private Integer id;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "quantity_change")
    private Integer quantityChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 30)
    private MovementReason reason;

    @Column(name = "quantity_after")
    private Integer quantityAfter;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "tracked_at", nullable = false)
    private Instant trackedAt;

    protected StockMovement() {
    }

    public StockMovement(Integer itemId, Integer statusId, String currentLocation, Integer quantityChange,
            MovementReason reason, Integer quantityAfter, String note, String performedBy, Instant trackedAt) {
        this.itemId = itemId;
        this.statusId = statusId;
        this.currentLocation = currentLocation;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.quantityAfter = quantityAfter;
        this.note = note;
        this.performedBy = performedBy;
        this.trackedAt = trackedAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public MovementReason getReason() {
        return reason;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public String getNote() {
        return note;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getTrackedAt() {
        return trackedAt;
    }
}
