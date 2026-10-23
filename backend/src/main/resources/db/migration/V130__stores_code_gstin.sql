-- T04 Multi-store: business store code (unique, required) and GSTIN on `store`.
-- Rows created before this migration (e.g. by earlier-versioned seeds) get a generated code first.
ALTER TABLE `store`
    ADD COLUMN `store_code` VARCHAR(20) NULL AFTER `store_id`,
    ADD COLUMN `gstin`      VARCHAR(15) NULL AFTER `store_location`;

-- LPAD truncates longer values, so only pad IDs below 10000.
UPDATE `store`
SET `store_code` = CONCAT('ST-', IF(`store_id` < 10000, LPAD(`store_id`, 4, '0'), `store_id`))
WHERE `store_code` IS NULL;

ALTER TABLE `store`
    MODIFY COLUMN `store_code` VARCHAR(20) NOT NULL,
    ADD UNIQUE KEY `uk_store_code` (`store_code`);
