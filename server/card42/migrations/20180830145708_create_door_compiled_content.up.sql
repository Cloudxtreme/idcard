BEGIN;

CREATE TABLE door_compiled_content (
  id               SERIAL PRIMARY KEY,
  door_id          INT         NOT NULL REFERENCES doors (id),

  patches          BYTEA [],
  unprotected_full BYTEA       NOT NULL,
  protected_full   BYTEA       NOT NULL,

  created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by       INT         NOT NULL REFERENCES users (id),

  activated      BOOLEAN     NOT NULL DEFAULT false,
  needs_approval BOOLEAN     NOT NULL DEFAULT true,
  approved_by    INT                  DEFAULT NULL REFERENCES users (id)
);

ALTER TABLE doors
  ADD CONSTRAINT active_config_fk
FOREIGN KEY (active_config_id)
REFERENCES door_compiled_content (id) NOT VALID;
ALTER TABLE doors
  VALIDATE CONSTRAINT active_config_fk;

COMMIT;