--liquibase formatted sql

--changeset tokens:001-create-token
create table token
(
    uid         varchar(36) primary key,
    contract_id varchar(64) not null,
    status      varchar(16) not null,
    valid_until date
);
--rollback drop table token;
