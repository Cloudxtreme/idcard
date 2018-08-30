
ALTER TABLE door_access_groups ALTER COLUMN action_order DROP DEFAULT;
DROP SEQUENCE door_access_groups_order_seq;

ALTER TABLE access_groups ALTER COLUMN card_group_id DROP DEFAULT;
DROP SEQUENCE access_groups_custom_id_seq;
