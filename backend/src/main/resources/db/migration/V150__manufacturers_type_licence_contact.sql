-- T06: trading-partner type (use cases Manufacturer / Supplier / Distributor), drug licence number and contact person.
ALTER TABLE `manufacturer`
    ADD COLUMN `manufacturer_type` VARCHAR(20)  NOT NULL DEFAULT 'MANUFACTURER' AFTER `manufacturer_name`,
    ADD COLUMN `licence_number`    VARCHAR(64)  NULL AFTER `manufacturer_type`,
    ADD COLUMN `contact_person`    VARCHAR(255) NULL AFTER `licence_number`,
    ADD CONSTRAINT `chk_manufacturer_type` CHECK (`manufacturer_type` IN ('MANUFACTURER', 'SUPPLIER', 'DISTRIBUTOR')),
    ADD UNIQUE KEY `uk_manufacturer_licence` (`licence_number`),
    ADD KEY `idx_manufacturer_type` (`manufacturer_type`);
