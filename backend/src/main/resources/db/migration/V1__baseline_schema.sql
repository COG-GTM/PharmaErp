-- =====================================================================================
-- V1 baseline schema (T00 scaffold). Creates EVERY table of the modernised Pharma ERP.
-- Sources: legacy Hibernate mapping (user, student — column-compatible with
-- parity/seed/legacy-schema.sql so parity/seed/*.sql loads unchanged) and the
-- "Database Schema" sheet of "ERP-Pharma Excel Sheets.xlsx" + "Pharma ERP USE-CASE.pdf".
-- Modules MUST NOT edit this file; schema changes / seed data go in each ticket's
-- reserved V<NNN>__<module>_<desc>.sql range (see docs/modernisation/CONVENTIONS.md).
-- Money is DECIMAL(12,2) (the sheet says double/varchar); camelCase -> snake_case.
-- =====================================================================================

-- ---------- Legacy (authentication) ----------------------------------------------------
CREATE TABLE `user` (
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `student` (
    `id`         INT          NOT NULL AUTO_INCREMENT,
    `first_name` VARCHAR(255) NULL,
    `last_name`  VARCHAR(255) NULL,
    `email`      VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Staff management ----------------------------------------------------------
-- Sheet "LogIn Type": jr.Employee, sr.Employee, Manager.
CREATE TABLE `login_type` (
    `login_id`   INT          NOT NULL AUTO_INCREMENT,
    `login_name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`login_id`),
    UNIQUE KEY `uk_login_type_name` (`login_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Multi-Store" (FK employeeId = store manager).
CREATE TABLE `store` (
    `store_id`            INT          NOT NULL AUTO_INCREMENT,
    `store_name`          VARCHAR(255) NOT NULL,
    `store_location`      VARCHAR(500) NULL,
    `manager_employee_id` INT          NULL,
    `created_at`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`store_id`),
    UNIQUE KEY `uk_store_name` (`store_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Staff Management" (FK loginId). staff_type covers the use-case actors
-- Staff(Supplier) / Staff(Distributor); username links the staff member to a system login.
CREATE TABLE `staff` (
    `employee_id`       INT           NOT NULL AUTO_INCREMENT,
    `login_id`          INT           NULL,
    `username`          VARCHAR(255)  NULL,
    `store_id`          INT           NULL,
    `staff_type`        VARCHAR(30)   NOT NULL DEFAULT 'INTERNAL',
    `first_name`        VARCHAR(100)  NOT NULL,
    `middle_name`       VARCHAR(100)  NULL,
    `last_name`         VARCHAR(100)  NULL,
    `date_of_birth`     DATE          NULL,
    `join_date`         DATE          NULL,
    `experience`        VARCHAR(255)  NULL,
    `salary`            DECIMAL(12,2) NULL,
    `home_town`         VARCHAR(255)  NULL,
    `home_town_address` VARCHAR(500)  NULL,
    `job_address`       VARCHAR(500)  NULL,
    `leaving_address`   VARCHAR(500)  NULL,
    `mobile_number`     VARCHAR(20)   NULL,
    `email`             VARCHAR(255)  NULL,
    `last_company`      VARCHAR(255)  NULL,
    `resume_path`       VARCHAR(500)  NULL,
    `feedback`          VARCHAR(1000) NULL,
    `suggestion`        VARCHAR(1000) NULL,
    `active`            BOOLEAN       NOT NULL DEFAULT TRUE,
    `created_at`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`employee_id`),
    UNIQUE KEY `uk_staff_username` (`username`),
    UNIQUE KEY `uk_staff_email` (`email`),
    KEY `idx_staff_store` (`store_id`),
    CONSTRAINT `fk_staff_login_type` FOREIGN KEY (`login_id`) REFERENCES `login_type` (`login_id`),
    CONSTRAINT `fk_staff_user` FOREIGN KEY (`username`) REFERENCES `user` (`username`)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT `fk_staff_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

ALTER TABLE `store`
    ADD CONSTRAINT `fk_store_manager` FOREIGN KEY (`manager_employee_id`) REFERENCES `staff` (`employee_id`)
        ON DELETE SET NULL;

-- Sheet "Security": tier allocation per employee (FK loginId, password via staff.username -> user).
CREATE TABLE `staff_security` (
    `employee_id`     INT         NOT NULL,
    `tier_allocation` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`employee_id`),
    CONSTRAINT `fk_staff_security_staff` FOREIGN KEY (`employee_id`) REFERENCES `staff` (`employee_id`)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Salary" (FK employeeId).
CREATE TABLE `salary` (
    `salary_id`     INT           NOT NULL AUTO_INCREMENT,
    `employee_id`   INT           NOT NULL,
    `salary_amount` DECIMAL(12,2) NOT NULL,
    `pay_period`    VARCHAR(7)    NULL COMMENT 'YYYY-MM',
    `paid_on`       DATE          NULL,
    `created_at`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`salary_id`),
    KEY `idx_salary_employee` (`employee_id`),
    CONSTRAINT `fk_salary_staff` FOREIGN KEY (`employee_id`) REFERENCES `staff` (`employee_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Parties --------------------------------------------------------------------
-- Sheet "Clients". Per-sale columns of the sheet (itemName, totalAmount, paidAmount,
-- clientInvoice) are normalised into invoice / invoice_line / payment.
CREATE TABLE `client` (
    `client_id`      INT          NOT NULL AUTO_INCREMENT,
    `client_name`    VARCHAR(255) NOT NULL,
    `client_address` VARCHAR(500) NULL,
    `client_number`  VARCHAR(20)  NULL,
    `client_email`   VARCHAR(255) NULL,
    `city`           VARCHAR(100) NULL,
    `district`       VARCHAR(100) NULL,
    `state`          VARCHAR(100) NULL,
    `gstin`          VARCHAR(15)  NULL,
    `feedback_email` VARCHAR(255) NULL,
    `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`client_id`),
    KEY `idx_client_name` (`client_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Manufacturer".
CREATE TABLE `manufacturer` (
    `manufacturer_id`      INT          NOT NULL AUTO_INCREMENT,
    `manufacturer_name`    VARCHAR(255) NOT NULL,
    `manufacturer_address` VARCHAR(500) NULL,
    `manufacturer_email`   VARCHAR(255) NULL,
    `manufacturer_phone`   VARCHAR(20)  NULL,
    `city`                 VARCHAR(100) NULL,
    `district`             VARCHAR(100) NULL,
    `state`                VARCHAR(100) NULL,
    `gstin`                VARCHAR(15)  NULL,
    `created_at`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`manufacturer_id`),
    UNIQUE KEY `uk_manufacturer_name` (`manufacturer_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Inventory ------------------------------------------------------------------
-- Sheet "Status": inTransit, arrived, processing, delivered.
CREATE TABLE `item_status` (
    `status_id`   INT         NOT NULL AUTO_INCREMENT,
    `status_name` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`status_id`),
    UNIQUE KEY `uk_item_status_name` (`status_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Inventory Management" (FK employeeId, statusId, clientId) + batch/expiry (FEFO),
-- barcode, manufacturer and store. item_delivery_date = arrival date.
CREATE TABLE `inventory_item` (
    `item_id`            INT           NOT NULL AUTO_INCREMENT,
    `item_name`          VARCHAR(255)  NOT NULL,
    `item_description`   VARCHAR(1000) NULL,
    `batch_no`           VARCHAR(64)   NULL,
    `expiry_date`        DATE          NULL,
    `quantity`           INT           NOT NULL DEFAULT 0,
    `unit_price`         DECIMAL(12,2) NULL,
    `gst_rate`           DECIMAL(5,2)  NULL COMMENT 'percent',
    `hsn_code`           VARCHAR(16)   NULL,
    `barcode`            VARCHAR(64)   NULL,
    `manufacturer_id`    INT           NULL,
    `store_id`           INT           NULL,
    `status_id`          INT           NULL,
    `employee_id`        INT           NULL,
    `client_id`          INT           NULL,
    `item_assign_date`   DATE          NULL,
    `item_delivery_date` DATE          NULL,
    `total_amount`       DECIMAL(12,2) NULL,
    `advance_amount`     DECIMAL(12,2) NULL,
    `created_at`         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`item_id`),
    KEY `idx_inventory_barcode` (`barcode`),
    KEY `idx_inventory_expiry` (`expiry_date`),
    KEY `idx_inventory_store` (`store_id`),
    KEY `idx_inventory_batch` (`batch_no`),
    CONSTRAINT `chk_inventory_quantity` CHECK (`quantity` >= 0),
    CONSTRAINT `fk_inventory_manufacturer` FOREIGN KEY (`manufacturer_id`) REFERENCES `manufacturer` (`manufacturer_id`),
    CONSTRAINT `fk_inventory_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
    CONSTRAINT `fk_inventory_status` FOREIGN KEY (`status_id`) REFERENCES `item_status` (`status_id`),
    CONSTRAINT `fk_inventory_staff` FOREIGN KEY (`employee_id`) REFERENCES `staff` (`employee_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_inventory_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Tracking" (FK itemId, status): movement / status history of an item.
CREATE TABLE `stock_tracking` (
    `tracking_id`      INT          NOT NULL AUTO_INCREMENT,
    `item_id`          INT          NOT NULL,
    `status_id`        INT          NULL,
    `current_location` VARCHAR(255) NULL,
    `quantity_change`  INT          NULL,
    `note`             VARCHAR(500) NULL,
    `tracked_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`tracking_id`),
    KEY `idx_tracking_item` (`item_id`, `tracked_at`),
    CONSTRAINT `fk_tracking_item` FOREIGN KEY (`item_id`) REFERENCES `inventory_item` (`item_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tracking_status` FOREIGN KEY (`status_id`) REFERENCES `item_status` (`status_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Use case "Prescription".
CREATE TABLE `prescription` (
    `prescription_id` INT          NOT NULL AUTO_INCREMENT,
    `client_id`       INT          NOT NULL,
    `doctor_name`     VARCHAR(255) NULL,
    `issued_on`       DATE         NULL,
    `notes`           VARCHAR(1000) NULL,
    `document_path`   VARCHAR(500) NULL,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`prescription_id`),
    KEY `idx_prescription_client` (`client_id`),
    CONSTRAINT `fk_prescription_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `prescription_line` (
    `prescription_line_id` INT          NOT NULL AUTO_INCREMENT,
    `prescription_id`      INT          NOT NULL,
    `item_id`              INT          NULL,
    `medicine_name`        VARCHAR(255) NOT NULL,
    `dosage`               VARCHAR(255) NULL,
    `quantity`             INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (`prescription_line_id`),
    CONSTRAINT `fk_prescription_line_prescription` FOREIGN KEY (`prescription_id`)
        REFERENCES `prescription` (`prescription_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_prescription_line_item` FOREIGN KEY (`item_id`) REFERENCES `inventory_item` (`item_id`)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Finance --------------------------------------------------------------------
-- Sheet "Invoice" + GST calculation use case.
CREATE TABLE `invoice` (
    `invoice_id`          INT           NOT NULL AUTO_INCREMENT,
    `invoice_number`      VARCHAR(32)   NOT NULL,
    `client_id`           INT           NULL,
    `store_id`            INT           NULL,
    `prescription_id`     INT           NULL,
    `invoice_description` VARCHAR(1000) NULL,
    `status`              VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    `invoice_date`        DATE          NOT NULL,
    `subtotal`            DECIMAL(12,2) NOT NULL DEFAULT 0,
    `gst_amount`          DECIMAL(12,2) NOT NULL DEFAULT 0,
    `total_amount`        DECIMAL(12,2) NOT NULL DEFAULT 0,
    `paid_amount`         DECIMAL(12,2) NOT NULL DEFAULT 0,
    `created_at`          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`invoice_id`),
    UNIQUE KEY `uk_invoice_number` (`invoice_number`),
    KEY `idx_invoice_client` (`client_id`),
    CONSTRAINT `fk_invoice_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`),
    CONSTRAINT `fk_invoice_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
    CONSTRAINT `fk_invoice_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescription` (`prescription_id`)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `invoice_line` (
    `invoice_line_id` INT           NOT NULL AUTO_INCREMENT,
    `invoice_id`      INT           NOT NULL,
    `item_id`         INT           NULL,
    `description`     VARCHAR(500)  NOT NULL,
    `quantity`        INT           NOT NULL,
    `unit_price`      DECIMAL(12,2) NOT NULL,
    `gst_rate`        DECIMAL(5,2)  NOT NULL DEFAULT 0 COMMENT 'percent',
    `gst_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0,
    `line_total`      DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (`invoice_line_id`),
    KEY `idx_invoice_line_invoice` (`invoice_id`),
    CONSTRAINT `chk_invoice_line_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `fk_invoice_line_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invoice_line_item` FOREIGN KEY (`item_id`) REFERENCES `inventory_item` (`item_id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Finance Management" (FK invoiceId). expense_category: electricityBill,
-- maintenanceBill, internetBill, waterSupplierBill, ...; GST_add -> gst_rate/gst_amount.
CREATE TABLE `expense` (
    `expense_id`          INT           NOT NULL AUTO_INCREMENT,
    `expense_category`    VARCHAR(50)   NOT NULL,
    `expense_description` VARCHAR(1000) NULL,
    `status`              VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    `amount`              DECIMAL(12,2) NOT NULL,
    `gst_rate`            DECIMAL(5,2)  NOT NULL DEFAULT 0 COMMENT 'percent',
    `gst_amount`          DECIMAL(12,2) NOT NULL DEFAULT 0,
    `expense_date`        DATE          NOT NULL,
    `invoice_id`          INT           NULL,
    `store_id`            INT           NULL,
    `created_at`          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`expense_id`),
    KEY `idx_expense_date` (`expense_date`),
    CONSTRAINT `fk_expense_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_expense_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Payments -------------------------------------------------------------------
-- Use case "Payment Method".
CREATE TABLE `payment_method` (
    `payment_method_id` INT         NOT NULL AUTO_INCREMENT,
    `method_name`       VARCHAR(50) NOT NULL,
    `active`            BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`payment_method_id`),
    UNIQUE KEY `uk_payment_method_name` (`method_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Sheet "Payment" (FK clientId) + use case "E-receipt" (receipt_number).
CREATE TABLE `payment` (
    `payment_id`        INT           NOT NULL AUTO_INCREMENT,
    `client_id`         INT           NULL,
    `invoice_id`        INT           NULL,
    `payment_method_id` INT           NULL,
    `payment_type`      VARCHAR(50)   NULL,
    `payment_amount`    DECIMAL(12,2) NOT NULL,
    `reference`         VARCHAR(100)  NULL,
    `receipt_number`    VARCHAR(32)   NULL,
    `paid_at`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`payment_id`),
    UNIQUE KEY `uk_payment_receipt` (`receipt_number`),
    KEY `idx_payment_invoice` (`invoice_id`),
    CONSTRAINT `chk_payment_amount` CHECK (`payment_amount` > 0),
    CONSTRAINT `fk_payment_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`),
    CONSTRAINT `fk_payment_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`),
    CONSTRAINT `fk_payment_method` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_method` (`payment_method_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Feedback -------------------------------------------------------------------
-- Sheet "Feedback" (FK employeeId, clientId).
CREATE TABLE `feedback` (
    `feedback_id`          INT           NOT NULL AUTO_INCREMENT,
    `employee_id`          INT           NULL,
    `client_id`            INT           NULL,
    `feedback_description` VARCHAR(2000) NOT NULL,
    `rating`               TINYINT       NULL,
    `created_at`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`feedback_id`),
    CONSTRAINT `chk_feedback_rating` CHECK (`rating` IS NULL OR `rating` BETWEEN 1 AND 5),
    CONSTRAINT `fk_feedback_staff` FOREIGN KEY (`employee_id`) REFERENCES `staff` (`employee_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_feedback_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
