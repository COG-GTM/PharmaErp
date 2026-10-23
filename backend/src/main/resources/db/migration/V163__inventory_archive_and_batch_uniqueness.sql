-- T07 Inventory: deleting a batch archives it so its stock_tracking history (ON DELETE CASCADE) survives,
-- and one active row per (barcode, batch, store) is enforced in the database, the unassigned (NULL) store included.
ALTER TABLE `inventory_item`
    ADD COLUMN `archived_at` TIMESTAMP NULL AFTER `updated_at`,
    ADD UNIQUE KEY `uq_inventory_active_batch` (
        `barcode`, `batch_no`, (CASE WHEN `archived_at` IS NULL THEN COALESCE(`store_id`, 0) END));
