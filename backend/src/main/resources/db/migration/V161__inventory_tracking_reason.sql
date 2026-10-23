-- T07 Inventory: per-item reorder level (drives the derived LOW status) and stock-movement audit
-- columns on the "Tracking" sheet table.
ALTER TABLE `inventory_item`
    ADD COLUMN `reorder_level` INT NULL AFTER `quantity`,
    ADD CONSTRAINT `chk_inventory_reorder_level` CHECK (`reorder_level` IS NULL OR `reorder_level` >= 0);

ALTER TABLE `stock_tracking`
    ADD COLUMN `reason`          VARCHAR(30)  NULL AFTER `quantity_change`,
    ADD COLUMN `quantity_after`  INT          NULL AFTER `reason`,
    ADD COLUMN `performed_by`    VARCHAR(255) NULL AFTER `note`;
