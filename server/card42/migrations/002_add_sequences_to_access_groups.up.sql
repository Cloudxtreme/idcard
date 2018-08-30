BEGIN;

CREATE SEQUENCE access_groups_custom_id_seq MINVALUE 512 MAXVALUE 65535 OWNED BY access_groups.card_group_id;
ALTER TABLE access_groups ALTER COLUMN card_group_id SET DEFAULT nextval('access_groups_custom_id_seq');

CREATE SEQUENCE door_access_groups_order_seq START WITH 1 OWNED BY door_access_groups.action_order;
ALTER TABLE door_access_groups ALTER COLUMN action_order SET DEFAULT nextval('door_access_groups_order_seq');

COMMIT;