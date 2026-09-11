-- Database.md §4.1 — identity_token. BR-CUS-03 (single-use, rotation-detectable).
CREATE TABLE identity_token (
    id           UUID        NOT NULL,
    account_id   UUID        NOT NULL,
    token_type   VARCHAR(24) NOT NULL,
    token_hash   TEXT        NOT NULL,
    issued_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    consumed_at  TIMESTAMPTZ,
    replaced_by  UUID,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID,
    CONSTRAINT pk_identity_token PRIMARY KEY (id),
    CONSTRAINT fk_identity_token_account_id
        FOREIGN KEY (account_id) REFERENCES identity_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_token_replaced_by
        FOREIGN KEY (replaced_by) REFERENCES identity_token (id),
    CONSTRAINT ck_identity_token_type
        CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'REFRESH')),
    CONSTRAINT ck_identity_token_expiry CHECK (expires_at > issued_at)
);
CREATE UNIQUE INDEX ux_identity_token_hash ON identity_token (token_hash);
CREATE INDEX ix_identity_token_account_id_type
    ON identity_token (account_id, token_type) WHERE consumed_at IS NULL;
