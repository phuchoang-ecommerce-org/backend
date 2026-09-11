-- Database.md §4.6 — payment_payment. BR-PAY-02 (refund never exceeds captured).
CREATE TABLE payment_payment (
    id                  UUID          NOT NULL,
    order_id            UUID          NOT NULL,
    customer_id         UUID          NOT NULL,
    method              VARCHAR(24)   NOT NULL,
    currency            CHAR(3)       NOT NULL,
    authorised_amount   NUMERIC(19,4) NOT NULL DEFAULT 0,
    captured_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    refunded_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    status              VARCHAR(24)   NOT NULL DEFAULT 'PENDING',
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          UUID,
    CONSTRAINT pk_payment_payment PRIMARY KEY (id),
    -- One Payment aggregate per order (Domain Model §8.6). Retries are
    -- additional attempts inside this aggregate, never additional payments.
    CONSTRAINT ux_payment_payment_order_id UNIQUE (order_id),
    CONSTRAINT ck_payment_payment_method
        CHECK (method IN ('CARD', 'BANK_TRANSFER', 'WALLET', 'CASH_ON_DELIVERY')),
    CONSTRAINT ck_payment_payment_status
        CHECK (status IN ('PENDING', 'AUTHORISED', 'CAPTURED', 'FAILED',
                          'PARTIALLY_REFUNDED', 'REFUNDED')),
    CONSTRAINT ck_payment_payment_amounts CHECK (
        authorised_amount >= 0 AND captured_amount >= 0 AND refunded_amount >= 0),
    -- BR-PAY-02: cumulative refunded amount never exceeds captured amount.
    CONSTRAINT ck_payment_payment_refund_bounded
        CHECK (refunded_amount <= captured_amount)
);
CREATE INDEX ix_payment_payment_customer_id_created_at
    ON payment_payment (customer_id, created_at DESC);
