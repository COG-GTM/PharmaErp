-- T04 Multi-store demo data: one store/warehouse per region. Idempotent on code or name.
INSERT INTO `store` (`store_code`, `store_name`, `store_location`, `gstin`)
SELECT 'MUM-01', 'PharmaERP Mumbai Central', '12 Linking Road, Bandra West, Mumbai, Maharashtra 400050', '27AABCP1234F1Z5'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `store` WHERE `store_code` = 'MUM-01' OR `store_name` = 'PharmaERP Mumbai Central');

INSERT INTO `store` (`store_code`, `store_name`, `store_location`, `gstin`)
SELECT 'AMD-01', 'PharmaERP Ahmedabad', '4 CG Road, Navrangpura, Ahmedabad, Gujarat 380009', '24AABCP1234F1Z1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `store` WHERE `store_code` = 'AMD-01' OR `store_name` = 'PharmaERP Ahmedabad');

INSERT INTO `store` (`store_code`, `store_name`, `store_location`, `gstin`)
SELECT 'BLR-WH1', 'PharmaERP Bengaluru Warehouse', 'Plot 18, Peenya Industrial Area, Bengaluru, Karnataka 560058', '29AABCP1234F1Z3'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `store` WHERE `store_code` = 'BLR-WH1' OR `store_name` = 'PharmaERP Bengaluru Warehouse');
