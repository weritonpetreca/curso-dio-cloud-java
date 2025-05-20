--liquibase formatted sql
--changeset weriton:202505151358
--comment: boards table create

CREATE TABLE BOARDS(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

--rollback DROP TABLE BOARDS