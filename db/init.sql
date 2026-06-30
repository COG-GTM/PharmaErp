-- Schema bootstrap for PharmaErp.
--
-- The Hibernate entities (com.schema.User / com.schema.Student) map to the
-- tables below. hibernate.cfg.xml leaves hbm2ddl.auto empty, so the schema is
-- created here instead of by Hibernate. MySQL runs this automatically the first
-- time the data volume is initialised (docker-entrypoint-initdb.d).

CREATE DATABASE IF NOT EXISTS PHARMA_ERP;
USE PHARMA_ERP;

CREATE TABLE IF NOT EXISTS `user` (
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`username`)
);

CREATE TABLE IF NOT EXISTS `student` (
    `id`         INT NOT NULL AUTO_INCREMENT,
    `first_name` VARCHAR(255),
    `last_name`  VARCHAR(255),
    `email`      VARCHAR(255),
    PRIMARY KEY (`id`)
);

-- Seed a demo login so the /login endpoint can be exercised immediately.
INSERT IGNORE INTO `user` (`username`, `password`) VALUES ('tarang', 'parisdkh');
