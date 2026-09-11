-- Database.md §4.5 — ordering_order. BR-ORD-01 legal states; BR-ORD-06 frozen amounts.
CREATE TABLE ordering_order (
    id                       UUID          NOT NULL,
    order_number             TEXT          NOT NULL,
    customer_id              UUID          NOT NULL,
    owner_id                 UUID,
    status                   VARCHAR(24)   NOT NULL DEFAULT 'DRAFT',
    currency                 CHAR(3)       NOT NULL,
    subtotal_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    shipping_fee_amount      NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount               NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount             NUMERIC(19,4) NOT NULL DEFAULT 0,
    promotion_id             UUID,
    promotion_code           TEXT,
    -- Address snapshots, not references. BR-ORD-06 freezes the order at
    -- placement, and a later edit to identity_address must not retroactively
    -- change where an order was shipped.
    shipping_recipient_name  TEXT,
    shipping_line1           TEXT,
    shipping_line2           TEXT,
    shipping_city            TEXT,
    shipping_region          TEXT,
    shipping_postal_code     TEXT,
    shipping_country_code    CHAR(2),
    billing_recipient_name   TEXT,
    billing_line1            TEXT,
    billing_line2            TEXT,
    billing_city             TEXT,
    billing_region           TEXT,
    billing_postal_code      TEXT,
    billing_country_code     CHAR(2),
    placed_at                TIMESTAMPTZ,
    paid_at                  TIMESTAMPTZ,
    delivered_at             TIMESTAMPTZ,
    return_window_ends_at    TIMESTAMPTZ,
    cancelled_reason         TEXT,
    version                  BIGINT        NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by               UUID,
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by               UUID,
    CONSTRAINT pk_ordering_order PRIMARY KEY (id),
    CONSTRAINT ux_ordering_order_number UNIQUE (order_number),
    -- BR-ORD-01: the legal state set. The transition *edges* are enforced by
    -- Order.transition() in the domain layer — a CHECK constraint sees one row
    -- at a time and cannot know the previous state. This constraint bounds the
    -- states; the aggregate bounds the moves between them.
    CONSTRAINT ck_ordering_order_status CHECK (status IN
        ('DRAFT', 'PENDING_PAYMENT', 'PAID', 'PAYMENT_FAILED', 'PROCESSING',
         'PACKED', 'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED',
         'RETURNED', 'REFUNDED')),
    CONSTRAINT ck_ordering_order_amounts CHECK (
        subtotal_amount     >= 0 AND
        discount_amount     >= 0 AND
        shipping_fee_amount >= 0 AND
        tax_amount          >= 0 AND
        total_amount        >= 0),
    -- BR-PRM-02: the order total is never negative and the discount never
    -- exceeds the discountable value. Promotion decides the discount; this is
    -- where the decision is checked against the order it was applied to.
    CONSTRAINT ck_ordering_order_discount_bounded CHECK (discount_amount <= subtotal_amount),
    CONSTRAINT ck_ordering_order_total CHECK (
        total_amount = subtotal_amount - discount_amount + shipping_fee_amount + tax_amount)
);
CREATE INDEX ix_ordering_order_customer_id_created_at
    ON ordering_order (customer_id, created_at DESC);
CREATE INDEX ix_ordering_order_status_created_at
    ON ordering_order (status, created_at DESC);
