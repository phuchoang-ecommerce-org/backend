-- Database.md §4.4 — cart_cart, cart_cart_line (child, same aggregate).
CREATE TABLE cart_cart (
    id                UUID        NOT NULL,
    customer_id       UUID,
    session_token     TEXT,
    status            VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    last_activity_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at        TIMESTAMPTZ NOT NULL,
    merged_into_id    UUID,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    CONSTRAINT pk_cart_cart PRIMARY KEY (id),
    CONSTRAINT fk_cart_cart_merged_into_id
        FOREIGN KEY (merged_into_id) REFERENCES cart_cart (id),
    CONSTRAINT ck_cart_cart_status
        CHECK (status IN ('ACTIVE', 'CHECKED_OUT', 'EXPIRED', 'MERGED')),
    -- A cart belongs to a customer or to a guest session, never to neither.
    CONSTRAINT ck_cart_cart_owner
        CHECK (customer_id IS NOT NULL OR session_token IS NOT NULL),
    CONSTRAINT ck_cart_cart_merged
        CHECK ((status = 'MERGED') = (merged_into_id IS NOT NULL))
);
-- One active cart per customer; one per guest session. Partial, because an
-- expired or checked-out cart must not block a new one.
CREATE UNIQUE INDEX ux_cart_cart_customer_active
    ON cart_cart (customer_id) WHERE status = 'ACTIVE' AND customer_id IS NOT NULL;
CREATE UNIQUE INDEX ux_cart_cart_session_active
    ON cart_cart (session_token) WHERE status = 'ACTIVE' AND session_token IS NOT NULL;
CREATE INDEX ix_cart_cart_expiry ON cart_cart (expires_at) WHERE status = 'ACTIVE';

CREATE TABLE cart_cart_line (
    id          UUID        NOT NULL,
    cart_id     UUID        NOT NULL,
    variant_id  UUID        NOT NULL,
    sku         TEXT        NOT NULL,
    quantity    INTEGER     NOT NULL,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID,
    CONSTRAINT pk_cart_cart_line PRIMARY KEY (id),
    CONSTRAINT fk_cart_cart_line_cart_id
        FOREIGN KEY (cart_id) REFERENCES cart_cart (id) ON DELETE CASCADE,
    CONSTRAINT ux_cart_cart_line_cart_variant UNIQUE (cart_id, variant_id),
    CONSTRAINT ck_cart_cart_line_quantity CHECK (quantity > 0)
);
