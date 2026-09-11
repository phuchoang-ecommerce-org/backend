-- Database.md §5.2 — ordering_processed_event. See catalog_create_processed_event
-- for why this is a per-module script rather than the shared one §8 names.
CREATE TABLE ordering_processed_event (
    event_id      UUID        NOT NULL,
    event_type    VARCHAR(64) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_ordering_processed_event PRIMARY KEY (event_id)
);
