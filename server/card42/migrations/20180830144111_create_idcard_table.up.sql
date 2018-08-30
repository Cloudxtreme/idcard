BEGIN;

CREATE TABLE cards (
  id             SERIAL PRIMARY KEY,
  card_serial    BIGINT UNIQUE NOT NULL,
  provision_date BIGINT        NOT NULL,
  provision_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  provision_by   INT           NOT NULL REFERENCES users (id),
  data_fmt_ver   INT           NOT NULL DEFAULT 1,
  owner_user_id  INT REFERENCES users (id)
);

CREATE TABLE card_compiled_content (
  id             SERIAL PRIMARY KEY,
  id_card_id     INT REFERENCES cards (id),
  files          BYTEA []    NOT NULL,
  creation_date  BIGINT      NOT NULL,
  creation_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creation_by    INT         NOT NULL REFERENCES users (id),

  activated      BOOLEAN     NOT NULL DEFAULT false,
  needs_approval BOOLEAN     NOT NULL DEFAULT true,
  approved_by    INT                  DEFAULT NULL REFERENCES users (id)
);

ALTER TABLE cards ADD COLUMN active_content_id INT REFERENCES card_compiled_content;

COMMIT;