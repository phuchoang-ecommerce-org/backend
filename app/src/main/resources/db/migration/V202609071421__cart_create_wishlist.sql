-- Database.md §4.4 — cart_wishlist, cart_wishlist_item.
CREATE TABLE cart_wishlist (
    id           UUID        NOT NULL,
    customer_id  UUID        NOT NULL,
    name         TEXT        NOT NULL DEFAULT 'Default',
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID,
    CONSTRAINT pk_cart_wishlist PRIMARY KEY (id),
    CONSTRAINT ux_cart_wishlist_customer_name UNIQUE (customer_id, name)
);

CREATE TABLE cart_wishlist_item (
    id           UUID        NOT NULL,
    wishlist_id  UUID        NOT NULL,
    variant_id   UUID        NOT NULL,
    added_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID,
    CONSTRAINT pk_cart_wishlist_item PRIMARY KEY (id),
    CONSTRAINT fk_cart_wishlist_item_wishlist_id
        FOREIGN KEY (wishlist_id) REFERENCES cart_wishlist (id) ON DELETE CASCADE,
    CONSTRAINT ux_cart_wishlist_item_wishlist_variant UNIQUE (wishlist_id, variant_id)
);
