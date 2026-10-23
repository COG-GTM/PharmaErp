-- T07 Inventory: logistics statuses from the spec's "Status" sheet (inTransit, arrived, processing, delivered).
-- Stock levels (IN_STOCK / LOW / OUT_OF_STOCK / EXPIRED) are derived at read time, not stored here.
INSERT IGNORE INTO `item_status` (`status_name`) VALUES
    ('IN_TRANSIT'),
    ('ARRIVED'),
    ('PROCESSING'),
    ('DELIVERED');
