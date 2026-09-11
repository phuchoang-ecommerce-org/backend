-- Database.md §4.5 — ordering_idempotency_key.
-- BR-ORD-03 / NFR-REL-02: repeated submission of the same confirmed checkout
-- yields one order. The primary key *is* the mechanism — the second concurrent
-- insert fails on the key rather than on a check the first transaction has not
-- yet committed. An application-level "have I seen this key" query cannot do
-- this: between its SELECT and its INSERT there is a window.
CREATE TABLE ordering_idempotency_key (
    idempotency_key  TEXT        NOT NULL,
    customer_id      UUID        NOT NULL,
    request_hash     TEXT        NOT NULL,
    order_id         UUID,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID,
    CONSTRAINT pk_ordering_idempotency_key PRIMARY KEY (idempotency_key),
    CONSTRAINT fk_ordering_idempotency_key_order_id
        FOREIGN KEY (order_id) REFERENCES ordering_order (id)
);
