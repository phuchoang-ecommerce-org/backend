-- Database.md §4.1 — identity_role, identity_account_role.
CREATE TABLE identity_role (
    id          UUID        NOT NULL,
    code        VARCHAR(32) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID,
    CONSTRAINT pk_identity_role PRIMARY KEY (id),
    CONSTRAINT ux_identity_role_code UNIQUE (code),
    CONSTRAINT ck_identity_role_code CHECK (code IN
        ('GUEST', 'CUSTOMER', 'STAFF', 'WAREHOUSE_OPERATOR',
         'CUSTOMER_SUPPORT', 'ADMINISTRATOR'))
);

CREATE TABLE identity_account_role (
    account_id  UUID        NOT NULL,
    role_id     UUID        NOT NULL,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by  UUID,
    CONSTRAINT pk_identity_account_role PRIMARY KEY (account_id, role_id),
    CONSTRAINT fk_identity_account_role_account_id
        FOREIGN KEY (account_id) REFERENCES identity_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_account_role_role_id
        FOREIGN KEY (role_id) REFERENCES identity_role (id)
);
CREATE INDEX ix_identity_account_role_role_id ON identity_account_role (role_id);
