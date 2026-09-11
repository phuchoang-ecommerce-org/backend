-- Database.md §4.11 — audit_entry, append-only by construction (ADR-0017).
CREATE TABLE audit_entry (
    id               UUID        NOT NULL,
    event_id         UUID        NOT NULL,
    actor_id         UUID,
    actor_role       VARCHAR(32),
    action           VARCHAR(64) NOT NULL,
    entity_type      VARCHAR(64) NOT NULL,
    entity_id        UUID,
    module           VARCHAR(32) NOT NULL,
    before_value     JSONB,
    after_value      JSONB,
    reason           TEXT,
    correlation_id   UUID,
    occurred_at      TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_entry PRIMARY KEY (id),
    -- At-least-once delivery means redelivery; the event id is the idempotency
    -- key ADR-0017 §4 requires, enforced here rather than checked in the handler.
    CONSTRAINT ux_audit_entry_event_id UNIQUE (event_id)
);
CREATE INDEX ix_audit_entry_entity ON audit_entry (entity_type, entity_id, occurred_at DESC);
CREATE INDEX ix_audit_entry_actor ON audit_entry (actor_id, occurred_at DESC);
CREATE INDEX ix_audit_entry_occurred_at ON audit_entry (occurred_at DESC);
CREATE INDEX ix_audit_entry_correlation_id ON audit_entry (correlation_id);

-- ADR-0017 §4: immutability does not rest on the application alone, and
-- ADR-0029 §4 requires these grants ship as a migration, since they have no
-- Java representation and are otherwise unreproducible across environments.
GRANT  INSERT, SELECT   ON audit_entry TO ecp_app;
REVOKE UPDATE, DELETE, TRUNCATE ON audit_entry FROM ecp_app;
