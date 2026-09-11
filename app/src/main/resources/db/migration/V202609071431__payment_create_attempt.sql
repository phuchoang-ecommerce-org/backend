-- Database.md §4.6 — payment_attempt, payment_refund. BR-PAY-01 (at most once per attempt).
CREATE TABLE payment_attempt (
    id                  UUID          NOT NULL,
    payment_id          UUID          NOT NULL,
    idempotency_key     TEXT          NOT NULL,
    attempt_number      INTEGER       NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            CHAR(3)       NOT NULL,
    outcome             VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    provider_reference  TEXT,
    provider_code       TEXT,
    failure_reason      TEXT,
    requested_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          UUID,
    CONSTRAINT pk_payment_attempt PRIMARY KEY (id),
    CONSTRAINT fk_payment_attempt_payment_id
        FOREIGN KEY (payment_id) REFERENCES payment_payment (id) ON DELETE CASCADE,
    -- BR-PAY-01: a provider result is applied at most once per attempt.
    -- Table-wide unique, so a redelivered callback carrying a key already seen
    -- collides regardless of which payment it claims to belong to.
    CONSTRAINT ux_payment_attempt_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ux_payment_attempt_number UNIQUE (payment_id, attempt_number),
    CONSTRAINT ck_payment_attempt_outcome
        CHECK (outcome IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_payment_attempt_resolution
        CHECK ((outcome = 'PENDING' AND resolved_at IS NULL)
            OR (outcome <> 'PENDING' AND resolved_at IS NOT NULL)),
    CONSTRAINT ck_payment_attempt_amount CHECK (amount > 0)
);
CREATE INDEX ix_payment_attempt_payment_id ON payment_attempt (payment_id);
CREATE INDEX ix_payment_attempt_provider_reference ON payment_attempt (provider_reference)
    WHERE provider_reference IS NOT NULL;

CREATE TABLE payment_refund (
    id                  UUID          NOT NULL,
    payment_id          UUID          NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            CHAR(3)       NOT NULL,
    reason              TEXT          NOT NULL,
    provider_reference  TEXT,
    status              VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    requested_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    settled_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          UUID,
    CONSTRAINT pk_payment_refund PRIMARY KEY (id),
    CONSTRAINT fk_payment_refund_payment_id
        FOREIGN KEY (payment_id) REFERENCES payment_payment (id) ON DELETE CASCADE,
    CONSTRAINT ck_payment_refund_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_refund_status
        CHECK (status IN ('PENDING', 'SETTLED', 'FAILED'))
);
CREATE INDEX ix_payment_refund_payment_id ON payment_refund (payment_id);
