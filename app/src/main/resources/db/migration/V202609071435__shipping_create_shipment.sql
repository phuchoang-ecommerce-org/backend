-- Database.md §4.7 — shipping_shipment, shipping_tracking_event (append-only child).
CREATE TABLE shipping_shipment (
    id                  UUID        NOT NULL,
    order_id            UUID        NOT NULL,
    carrier             VARCHAR(32) NOT NULL,
    tracking_reference  TEXT,
    status              VARCHAR(24) NOT NULL DEFAULT 'CREATED',
    status_rank         INTEGER     NOT NULL DEFAULT 0,
    destination_postal_code TEXT,
    destination_country_code CHAR(2),
    dispatched_at       TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID,
    CONSTRAINT pk_shipping_shipment PRIMARY KEY (id),
    CONSTRAINT ck_shipping_shipment_status CHECK (status IN
        ('CREATED', 'DISPATCHED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY',
         'DELIVERED', 'FAILED', 'RETURNED')),
    CONSTRAINT ck_shipping_shipment_status_rank CHECK (status_rank >= 0)
);
CREATE INDEX ix_shipping_shipment_order_id ON shipping_shipment (order_id);
CREATE UNIQUE INDEX ux_shipping_shipment_carrier_tracking
    ON shipping_shipment (carrier, tracking_reference)
    WHERE tracking_reference IS NOT NULL;

CREATE TABLE shipping_tracking_event (
    id                  UUID        NOT NULL,
    shipment_id         UUID        NOT NULL,
    carrier_event_id    TEXT,
    status              VARCHAR(24) NOT NULL,
    status_rank         INTEGER     NOT NULL,
    description         TEXT,
    location            TEXT,
    -- When the carrier says it happened, not when we received it. An
    -- out-of-order delivery is ordinary, and BR-SHP-02's comparison is against
    -- this column; received_at exists only to explain a gap after the fact.
    carrier_occurred_at TIMESTAMPTZ NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    applied             BOOLEAN     NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    CONSTRAINT pk_shipping_tracking_event PRIMARY KEY (id),
    CONSTRAINT fk_shipping_tracking_event_shipment_id
        FOREIGN KEY (shipment_id) REFERENCES shipping_shipment (id) ON DELETE CASCADE,
    CONSTRAINT ck_shipping_tracking_event_status_rank CHECK (status_rank >= 0)
);
CREATE INDEX ix_shipping_tracking_event_shipment_id
    ON shipping_tracking_event (shipment_id, carrier_occurred_at DESC);
CREATE UNIQUE INDEX ux_shipping_tracking_event_carrier_event_id
    ON shipping_tracking_event (shipment_id, carrier_event_id)
    WHERE carrier_event_id IS NOT NULL;
