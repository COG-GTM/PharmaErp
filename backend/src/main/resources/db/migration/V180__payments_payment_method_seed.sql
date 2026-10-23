-- T09 Payments: reference/demo payment methods (use case "Payment Method").
-- INSERT IGNORE keeps the migration safe on databases where a method was already added by hand.
INSERT IGNORE INTO `payment_method` (`method_name`, `active`) VALUES
    ('Cash', TRUE),
    ('Debit Card', TRUE),
    ('Credit Card', TRUE),
    ('UPI', TRUE),
    ('Net Banking', TRUE),
    ('Bank Transfer (NEFT/RTGS)', TRUE),
    ('Cheque', TRUE),
    ('Demand Draft', FALSE);
