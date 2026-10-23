-- Legacy schema as mapped by com.schema.User / com.schema.Student (hbm2ddl.auto is empty in hibernate.cfg.xml).
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
