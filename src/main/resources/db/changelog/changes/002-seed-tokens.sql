--liquibase formatted sql

--changeset tokens:002-seed-tokens
insert into token (uid, contract_id, status, valid_until)
values ('04A2B3C4D5E6F7', 'DE-DCS-C12345678-X', 'ACTIVE', '2027-12-31'),
       ('04B1B2B3B4B5B6', 'DE-DCS-C87654321-Y', 'ACTIVE', null),
       ('04C1C2C3C4C5C6', 'DE-DCS-C11112222-Z', 'BLOCKED', '2027-12-31'),
       ('04D1D2D3D4D5D6', 'DE-DCS-C33334444-W', 'ACTIVE', '2025-12-31');
--rollback delete from token where uid in ('04A2B3C4D5E6F7','04B1B2B3B4B5B6','04C1C2C3C4C5C6','04D1D2D3D4D5D6');
