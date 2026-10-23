-- T08 Finance demo seed: three invoices (paid, partially paid, issued) and four expenses.
-- Amounts follow GstCalculator: per line taxable = qty * unit_price; CGST = SGST = taxable * rate/2 %
-- rounded HALF_UP to 2 decimals; gst_amount = CGST + SGST. Client/store/item ids are left NULL so the seed
-- does not depend on other modules' seed data.

INSERT INTO `invoice` (`invoice_number`, `invoice_description`, `status`, `invoice_date`,
                       `subtotal`, `gst_amount`, `total_amount`, `paid_amount`)
VALUES ('SEED-FIN-1', 'Demo: retail counter sale', 'PAID', '2026-01-15', 802.93, 100.30, 903.23, 903.23);
SET @inv1 = LAST_INSERT_ID();
INSERT INTO `invoice_line` (`invoice_id`, `description`, `quantity`, `unit_price`, `gst_rate`, `gst_amount`, `line_total`)
VALUES (@inv1, 'Paracetamol 500mg (strip of 10)', 10, 25.50, 12.00, 30.60, 285.60),
       (@inv1, 'Vitamin C 500mg tablets (bottle of 60)', 3, 120.00, 18.00, 64.80, 424.80),
       (@inv1, 'Insulin syringe 1ml', 7, 13.99, 5.00, 4.90, 102.83),
       (@inv1, 'Cotton wool roll 100g', 2, 45.00, 0.00, 0.00, 90.00);

INSERT INTO `invoice` (`invoice_number`, `invoice_description`, `status`, `invoice_date`,
                       `subtotal`, `gst_amount`, `total_amount`, `paid_amount`)
VALUES ('SEED-FIN-2', 'Demo: clinic bulk order', 'PARTIALLY_PAID', '2026-02-03', 12682.94, 1984.12, 14667.06, 5000.00);
SET @inv2 = LAST_INSERT_ID();
INSERT INTO `invoice_line` (`invoice_id`, `description`, `quantity`, `unit_price`, `gst_rate`, `gst_amount`, `line_total`)
VALUES (@inv2, 'Nitrile examination gloves (box of 100)', 20, 349.00, 12.00, 837.60, 7817.60),
       (@inv2, 'Hand sanitiser 5L', 4, 1125.75, 18.00, 810.54, 5313.54),
       (@inv2, 'Cosmetic moisturising cream 200ml', 6, 199.99, 28.00, 335.98, 1535.92);

INSERT INTO `invoice` (`invoice_number`, `invoice_description`, `status`, `invoice_date`,
                       `subtotal`, `gst_amount`, `total_amount`, `paid_amount`)
VALUES ('SEED-FIN-3', 'Demo: home delivery', 'ISSUED', '2026-02-20', 89.50, 10.74, 100.24, 0.00);
SET @inv3 = LAST_INSERT_ID();
INSERT INTO `invoice_line` (`invoice_id`, `description`, `quantity`, `unit_price`, `gst_rate`, `gst_amount`, `line_total`)
VALUES (@inv3, 'Cough syrup 100ml', 1, 89.50, 12.00, 10.74, 100.24);

UPDATE `invoice`
SET `invoice_number` = CONCAT('INV-', LPAD(`invoice_id`, 6, '0'))
WHERE `invoice_id` IN (@inv1, @inv2, @inv3);

INSERT INTO `expense` (`expense_category`, `expense_description`, `status`, `amount`, `gst_rate`, `gst_amount`,
                       `expense_date`, `invoice_id`)
VALUES ('electricityBill', 'Demo: January electricity', 'PAID', 4500.00, 18.00, 810.00, '2026-01-31', NULL),
       ('maintenanceBill', 'Demo: refrigeration unit service', 'PAID', 1200.00, 18.00, 216.00, '2026-02-05', NULL),
       ('internetBill', 'Demo: February broadband', 'PENDING', 999.00, 18.00, 179.82, '2026-02-10', NULL),
       ('waterSupplierBill', 'Demo: February water supply', 'PENDING', 650.00, 0.00, 0.00, '2026-02-12', NULL);
