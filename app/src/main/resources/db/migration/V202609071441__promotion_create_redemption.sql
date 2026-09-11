-- Database.md §4.8 — promotion_redemption.
CREATE TABLE promotion_redemption (
    id               UUID          NOT NULL,
    promotion_id     UUID          NOT NULL,
    order_id         UUID          NOT NULL,
    customer_id      UUID          NOT NULL,
    discount_amount  NUMERIC(19,4) NOT NULL,
    currency         CHAR(3)       NOT NULL,
    redeemed_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       UUID,
    CONSTRAINT pk_promotion_redemption PRIMARY KEY (id),
    CONSTRAINT fk_promotion_redemption_promotion_id
        FOREIGN KEY (promotion_id) REFERENCES promotion_promotion (id),
    -- One redemption of a given promotion per order — the replay guard that
    -- pairs with ordering_idempotency_key on the other side of the Partnership.
    CONSTRAINT ux_promotion_redemption_promotion_order UNIQUE (promotion_id, order_id),
    CONSTRAINT ck_promotion_redemption_amount CHECK (discount_amount >= 0)
);
CREATE INDEX ix_promotion_redemption_customer
    ON promotion_redemption (promotion_id, customer_id);
