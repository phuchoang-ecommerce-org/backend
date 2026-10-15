-- Sprint 17: persistent Draft-checkout state and one active checkout per customer.
ALTER TABLE ordering_order
    ADD COLUMN checkout_shipping_option_id TEXT,
    ADD COLUMN checkout_shipping_carrier TEXT,
    ADD COLUMN checkout_shipping_service TEXT,
    ADD COLUMN checkout_estimated_delivery_from TIMESTAMPTZ,
    ADD COLUMN checkout_estimated_delivery_to TIMESTAMPTZ,
    ADD COLUMN checkout_confirmation_token TEXT;

CREATE UNIQUE INDEX ux_ordering_order_customer_draft
    ON ordering_order (customer_id) WHERE status = 'DRAFT';
