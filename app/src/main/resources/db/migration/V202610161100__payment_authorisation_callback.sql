-- Sprint 21: Payment owns its OrderCreated snapshot and reconciliation evidence.
-- No foreign key reaches ordering: the relationship is the durable event contract.
CREATE TABLE payment_order_snapshot (
    order_id UUID PRIMARY KEY, customer_id UUID NOT NULL, total_amount NUMERIC(19,4) NOT NULL CHECK (total_amount > 0),
    currency CHAR(3) NOT NULL, status VARCHAR(24) NOT NULL, selected_method VARCHAR(24), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE payment_checkout_selection (
    checkout_id UUID PRIMARY KEY, customer_id UUID NOT NULL, method VARCHAR(24) NOT NULL,
    selected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_checkout_selection_method CHECK (method IN ('CARD', 'BANK_TRANSFER', 'WALLET', 'CASH_ON_DELIVERY'))
);
CREATE TABLE payment_unmatched_notification (
    id UUID PRIMARY KEY, provider_event_id TEXT, provider_reference TEXT NOT NULL, outcome VARCHAR(16) NOT NULL,
    amount NUMERIC(19,4), currency CHAR(3), received_at TIMESTAMPTZ NOT NULL, resolved_at TIMESTAMPTZ,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb, CONSTRAINT ux_payment_unmatched_provider_event UNIQUE (provider_event_id),
    CONSTRAINT ck_payment_unmatched_outcome CHECK (outcome IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);
CREATE TABLE payment_processed_provider_event (provider_event_id TEXT PRIMARY KEY, processed_at TIMESTAMPTZ NOT NULL DEFAULT now());
