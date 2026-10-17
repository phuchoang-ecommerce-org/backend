-- Stable keyset traversal of the reconciliation queue, including tied timestamps.
CREATE INDEX ix_payment_unmatched_received_id
    ON payment_unmatched_notification (received_at DESC, id DESC);
