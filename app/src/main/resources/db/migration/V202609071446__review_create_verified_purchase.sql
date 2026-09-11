-- Database.md §4.9 — review_verified_purchase. Review's own projection, built
-- from OrderDelivered / OrderCompleted (Domain Model §5.2). Owned and written
-- only by review's event handler; BR-REV-01's check is a PK lookup on this table.
CREATE TABLE review_verified_purchase (
    customer_id   UUID        NOT NULL,
    product_id    UUID        NOT NULL,
    order_id      UUID        NOT NULL,
    delivered_at  TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_review_verified_purchase PRIMARY KEY (customer_id, product_id, order_id)
);
