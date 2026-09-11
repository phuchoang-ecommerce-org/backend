-- Database.md §5.1 — ordering_outbox. Envelope columns match Integration
-- Contract §6.1 column for column. No PARTITION BY: Backend Architecture.md §11
-- notes monthly partitioning was "applied" elsewhere, but the literal DDL in
-- Database.md §5.1 carries no partitioning clause — followed as written here.
CREATE TABLE ordering_outbox (
    -- The relay's total order. occurred_at is a business timestamp and two
    -- events emitted in one transaction can share it, so BIGSERIAL gives the
    -- total order the relay and replay both actually need (Backend
    -- Architecture.md §3.4.3).
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
    CONSTRAINT pk_ordering_outbox PRIMARY KEY (event_id),
    CONSTRAINT ck_ordering_outbox_event_version CHECK (event_version >= 1),
    CONSTRAINT ck_ordering_outbox_attempt_count CHECK (attempt_count >= 0)
);

-- The partial index ADR-0009 §4 names by example. Unpublished rows are a small,
-- roughly constant working set; published rows are the whole history of the
-- platform. Indexing only the former keeps the relay's poll O(backlog) rather
-- than O(events ever published).
CREATE INDEX ix_ordering_outbox_unpublished
    ON ordering_outbox (sequence_no) WHERE published_at IS NULL;

-- Replay reads published rows in the same total order (Backend Architecture.md
-- §3.4.5), so the sequence is unique per table rather than merely monotonic.
ALTER TABLE ordering_outbox
    ADD CONSTRAINT ux_ordering_outbox_sequence_no UNIQUE (sequence_no);
