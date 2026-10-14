-- Generated voucher codes need their own lifecycle; a single column on the
-- campaign cannot represent a secure batch or a single-use code.
CREATE TABLE promotion_voucher (
    id           UUID        NOT NULL,
    promotion_id UUID        NOT NULL,
    code         TEXT        NOT NULL,
    single_use   BOOLEAN     NOT NULL DEFAULT true,
    redeemed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_promotion_voucher PRIMARY KEY (id),
    CONSTRAINT fk_promotion_voucher_promotion_id
        FOREIGN KEY (promotion_id) REFERENCES promotion_promotion (id),
    CONSTRAINT ux_promotion_voucher_code UNIQUE (code)
);
CREATE INDEX ix_promotion_voucher_promotion ON promotion_voucher (promotion_id);
ALTER TABLE promotion_redemption ADD COLUMN voucher_code TEXT;
CREATE UNIQUE INDEX ux_promotion_redemption_voucher_single_use
    ON promotion_redemption (voucher_code) WHERE voucher_code IS NOT NULL;
