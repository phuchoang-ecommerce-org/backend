ALTER TABLE promotion_promotion
    ADD COLUMN scheduled_status VARCHAR(16),
    ADD COLUMN scheduled_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_promotion_promotion_scheduled_status
        CHECK (scheduled_status IS NULL OR scheduled_status IN ('ACTIVE', 'PAUSED', 'EXPIRED')),
    ADD CONSTRAINT ck_promotion_promotion_schedule_pair
        CHECK ((scheduled_status IS NULL) = (scheduled_at IS NULL));

CREATE INDEX ix_promotion_promotion_scheduled_status
    ON promotion_promotion (scheduled_at)
    WHERE scheduled_status IS NOT NULL;
