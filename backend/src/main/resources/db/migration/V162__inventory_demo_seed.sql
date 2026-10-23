-- T07 Inventory: demo stock. Store/manufacturer links resolve to the first rows seeded by their own
-- modules when present (NULL otherwise). Dates are relative to the migration date so the
-- expiring / expired / low-stock views have data to show.
INSERT INTO `inventory_item`
    (`item_name`, `item_description`, `batch_no`, `expiry_date`, `quantity`, `reorder_level`, `unit_price`,
     `gst_rate`, `hsn_code`, `barcode`, `manufacturer_id`, `store_id`, `status_id`, `item_delivery_date`)
SELECT d.item_name, d.item_description, d.batch_no, d.expiry_date, d.quantity, d.reorder_level, d.unit_price,
       d.gst_rate, d.hsn_code, d.barcode,
       (SELECT MIN(`manufacturer_id`) FROM `manufacturer`),
       (SELECT MIN(`store_id`) FROM `store`),
       (SELECT `status_id` FROM `item_status` WHERE `status_name` = d.status_name),
       d.delivery_date
FROM (
    SELECT 'Paracetamol 500mg Tablets' AS item_name, 'Strip of 10 tablets' AS item_description,
           'DEMO-PCM-2401' AS batch_no, DATE_ADD(CURDATE(), INTERVAL 540 DAY) AS expiry_date, 480 AS quantity,
           50 AS reorder_level, 18.50 AS unit_price, 12.00 AS gst_rate, '30049099' AS hsn_code,
           '8901234500017' AS barcode, 'ARRIVED' AS status_name, DATE_SUB(CURDATE(), INTERVAL 30 DAY) AS delivery_date
    UNION ALL SELECT 'Amoxicillin 250mg Capsules', 'Strip of 15 capsules', 'DEMO-AMX-2402',
           DATE_ADD(CURDATE(), INTERVAL 20 DAY), 35, 40, 62.00, 12.00, '30041030', '8901234500024', 'ARRIVED',
           DATE_SUB(CURDATE(), INTERVAL 200 DAY)
    UNION ALL SELECT 'Cetirizine 10mg Tablets', 'Strip of 10 tablets', 'DEMO-CTZ-2403',
           DATE_ADD(CURDATE(), INTERVAL 365 DAY), 0, 25, 24.00, 12.00, '30049099', '8901234500031', 'DELIVERED',
           DATE_SUB(CURDATE(), INTERVAL 90 DAY)
    UNION ALL SELECT 'Cough Syrup 100ml', 'Bottle, 100 ml', 'DEMO-CSY-2311',
           DATE_SUB(CURDATE(), INTERVAL 5 DAY), 12, 10, 95.00, 12.00, '30049011', '8901234500048', 'ARRIVED',
           DATE_SUB(CURDATE(), INTERVAL 400 DAY)
    UNION ALL SELECT 'Insulin Glargine 100IU/ml', 'Pre-filled pen, cold chain', 'DEMO-INS-2405',
           DATE_ADD(CURDATE(), INTERVAL 120 DAY), 60, 20, 780.00, 5.00, '30043110', '8901234500055', 'IN_TRANSIT',
           NULL
) d;

INSERT INTO `stock_tracking` (`item_id`, `status_id`, `current_location`, `quantity_change`, `reason`,
                              `quantity_after`, `note`, `performed_by`)
SELECT i.`item_id`, i.`status_id`, 'Demo seed', i.`quantity`, 'RECEIPT', i.`quantity`, 'Initial stock (demo seed)',
       'system'
FROM `inventory_item` i
WHERE i.`batch_no` LIKE 'DEMO-%';
