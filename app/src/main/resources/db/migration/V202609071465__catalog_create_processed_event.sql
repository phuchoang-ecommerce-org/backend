-- Database.md §5.2 — catalog_processed_event. Consumer-side idempotency storage
-- for a Kafka consumer with no natural dedupe key.
--
-- Deliberately one script per consuming module, not the single
-- V...__shared_create_processed_event.sql the §8 migration map names: §11 Open
-- Question 7 flags that a single script creating a same-shaped table for
-- several modules violates ADR-0029's one-script-one-module rule, and
-- recommends the split adopted here (see also V...__ordering_create_processed_
-- event.sql, V...__shipping_create_processed_event.sql,
-- V...__reporting_create_processed_event.sql).
CREATE TABLE catalog_processed_event (
    event_id      UUID        NOT NULL,
    event_type    VARCHAR(64) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_catalog_processed_event PRIMARY KEY (event_id)
);
-- ix_..._processed_at deliberately absent: nothing queries this table by time.
-- It is pruned by an operational job on a retention window longer than the
-- maximum Kafka redelivery horizon (§11).
