-- Database.md §4.8 — promotion_promotion. Structurally mirrors inventory_stock_item's
-- oversell defence for redemption-slot claiming.
CREATE TABLE promotion_promotion (
    id                  UUID          NOT NULL,
    code                TEXT,
    name                TEXT          NOT NULL,
    description         TEXT,
    discount_type       VARCHAR(16)   NOT NULL,
    discount_value      NUMERIC(19,4) NOT NULL,
    max_discount_amount NUMERIC(19,4),
    min_order_amount    NUMERIC(19,4),
    currency            CHAR(3),
    discount_rule       JSONB         NOT NULL DEFAULT '{}'::jsonb,
    valid_from          TIMESTAMPTZ   NOT NULL,
    valid_until         TIMESTAMPTZ   NOT NULL,
    usage_limit         INTEGER,
    usage_count         INTEGER       NOT NULL DEFAULT 0,
    per_customer_limit  INTEGER,
    priority            INTEGER       NOT NULL DEFAULT 0,
    stackable           BOOLEAN       NOT NULL DEFAULT false,
    status              VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          UUID,
    CONSTRAINT pk_promotion_promotion PRIMARY KEY (id),
    CONSTRAINT ux_promotion_promotion_code UNIQUE (code),
    CONSTRAINT ck_promotion_promotion_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SHIPPING')),
    CONSTRAINT ck_promotion_promotion_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'EXPIRED')),
    CONSTRAINT ck_promotion_promotion_validity CHECK (valid_until > valid_from),
    CONSTRAINT ck_promotion_promotion_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_promotion_promotion_percentage_bounded
        CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT ck_promotion_promotion_usage_count CHECK (usage_count >= 0),
    -- The usage cap. Structurally identical to Inventory's oversell defence,
    -- for the reason UC-PRM-02 E7 gives: over-redemption is unbudgeted spend,
    -- so it holds under concurrency the same way BR-INV-01 does.
    CONSTRAINT ck_promotion_promotion_usage_bounded
        CHECK (usage_limit IS NULL OR usage_count <= usage_limit)
);
-- Active-promotion lookup at cart preview and at placement. Partial, because
-- expired and draft promotions are never evaluated and would otherwise
-- accumulate in the index for the life of the platform.
CREATE INDEX ix_promotion_promotion_active
    ON promotion_promotion (valid_from, valid_until)
    WHERE status = 'ACTIVE';
CREATE INDEX ix_promotion_promotion_priority
    ON promotion_promotion (priority DESC) WHERE status = 'ACTIVE';
