-- Database.md §4.1 — identity_account.
CREATE TABLE identity_account (
    id                   UUID        NOT NULL,
    email                TEXT        NOT NULL,
    credential_hash      TEXT        NOT NULL,
    display_name         TEXT,
    status               VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    verification_status  VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    verified_at          TIMESTAMPTZ,
    last_login_at        TIMESTAMPTZ,
    failed_login_count   INTEGER     NOT NULL DEFAULT 0,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           UUID,
    CONSTRAINT pk_identity_account PRIMARY KEY (id),
    CONSTRAINT ck_identity_account_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ck_identity_account_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_identity_account_failed_login_count
        CHECK (failed_login_count >= 0)
);

-- BR-CUS-01: email identifies at most one account.
-- Expression index, not a plain UNIQUE: email equality is case-insensitive in
-- every use case that compares one, so uniqueness must be too, or two accounts
-- differing only in case defeat the rule the constraint exists to enforce.
CREATE UNIQUE INDEX ux_identity_account_email ON identity_account (lower(email));

CREATE TABLE identity_address (
    id                   UUID        NOT NULL,
    account_id           UUID        NOT NULL,
    label                TEXT,
    recipient_name       TEXT        NOT NULL,
    line1                TEXT        NOT NULL,
    line2                TEXT,
    city                 TEXT        NOT NULL,
    region               TEXT,
    postal_code          TEXT        NOT NULL,
    country_code         CHAR(2)     NOT NULL,
    phone                TEXT,
    is_default_shipping  BOOLEAN     NOT NULL DEFAULT false,
    is_default_billing   BOOLEAN     NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           UUID,
    CONSTRAINT pk_identity_address PRIMARY KEY (id),
    CONSTRAINT fk_identity_address_account_id
        FOREIGN KEY (account_id) REFERENCES identity_account (id) ON DELETE CASCADE
);

-- BR-CUS-05: at most one default shipping address per account.
-- A partial unique index expresses "at most one true per account" directly;
-- a plain UNIQUE (account_id, is_default_shipping) would instead forbid a
-- second *non*-default address, which is not the rule.
CREATE UNIQUE INDEX ux_identity_address_default_shipping
    ON identity_address (account_id) WHERE is_default_shipping;
CREATE UNIQUE INDEX ux_identity_address_default_billing
    ON identity_address (account_id) WHERE is_default_billing;
CREATE INDEX ix_identity_address_account_id ON identity_address (account_id);
