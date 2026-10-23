-- T09 payments / INT-W2: payments recorded before invoice settlement existed never touched invoice.paid_amount.
-- Fold them in once (on top of any opening paid amount, which those payments were independent of) and
-- re-derive the status. Cancelled invoices keep their status.
UPDATE `invoice` i
    JOIN (SELECT `invoice_id`, SUM(`payment_amount`) AS `paid`
          FROM `payment`
          WHERE `invoice_id` IS NOT NULL
          GROUP BY `invoice_id`) p ON p.`invoice_id` = i.`invoice_id`
-- MySQL applies SET assignments left to right, so derive the status before paid_amount changes.
SET i.`status` = CASE
        WHEN i.`status` = 'CANCELLED' THEN i.`status`
        WHEN i.`paid_amount` + p.`paid` >= i.`total_amount` THEN 'PAID'
        WHEN i.`paid_amount` + p.`paid` > 0 THEN 'PARTIALLY_PAID'
        ELSE i.`status`
    END,
    i.`paid_amount` = i.`paid_amount` + p.`paid`;
