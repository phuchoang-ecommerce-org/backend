-- Database.md §5.1 — promotion_outbox. Same shape as ordering_outbox, this module's prefix.
CREATE TABLE promotion_outbox (
    sequence_no     BIGSERIAL   NOT NULL,
    event_id        UUID        NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    event_version   INTEGER     NOT NULL DEFAULT 1,
    occurred_at     TIMESTAMPTZ NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID        NOT NULL,
    correlation_id  UUID        NOT NULL,
    actor_user_id   UUID,
    actor_role      VARCHAR(32),
    payload         JSONB       NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    published_at    TIMESTAMPTZ,
    attempt_count   INTEGER     NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_promotion_outbox PRIMARY KEY (event_id),
    CONSTRAINT ck_promotion_outbox_event_version CHECK (event_version >= 1),
    CONSTRAINT ck_promotion_outbox_attempt_count CHECK (attempt_count >= 0)
);
CREATE INDEX ix_promotion_outbox_unpublished
    ON promotion_outbox (sequence_no) WHERE published_at IS NULL;
ALTER TABLE promotion_outbox
    ADD CONSTRAINT ux_promotion_outbox_sequence_no UNIQUE (sequence_no);
