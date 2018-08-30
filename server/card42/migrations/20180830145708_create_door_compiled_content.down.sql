BEGIN;

ALTER TABLE doors DROP CONSTRAINT active_config_fk;
DROP TABLE door_compiled_content;

COMMIT;