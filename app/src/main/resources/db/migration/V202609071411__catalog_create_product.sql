-- Database.md §4.2 — catalog_product, catalog_product_image (child, same aggregate).
CREATE TABLE catalog_product (
    id                  UUID        NOT NULL,
    category_id         UUID        NOT NULL,
    owner_id            UUID,
    name                TEXT        NOT NULL,
    slug                TEXT        NOT NULL,
    description         TEXT,
    brand               TEXT,
    publication_status  VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at        TIMESTAMPTZ,
    attributes          JSONB       NOT NULL DEFAULT '{}'::jsonb,
    average_rating      NUMERIC(3,2),
    review_count        INTEGER     NOT NULL DEFAULT 0,
    -- The rating projection's ordering guard (CQRS.md §5.1, §6.2). Carries the
    -- envelope's occurredAt, never the write time.
    rating_last_event_at TIMESTAMPTZ,
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID,
    CONSTRAINT pk_catalog_product PRIMARY KEY (id),
    CONSTRAINT fk_catalog_product_category_id
        FOREIGN KEY (category_id) REFERENCES catalog_category (id),
    CONSTRAINT ux_catalog_product_slug UNIQUE (slug),
    CONSTRAINT ck_catalog_product_publication_status
        CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'DISCONTINUED')),
    CONSTRAINT ck_catalog_product_review_count CHECK (review_count >= 0),
    CONSTRAINT ck_catalog_product_average_rating
        CHECK (average_rating IS NULL OR (average_rating >= 1 AND average_rating <= 5))
);
CREATE INDEX ix_catalog_product_category_id ON catalog_product (category_id)
    WHERE publication_status = 'PUBLISHED';

CREATE TABLE catalog_product_image (
    id          UUID        NOT NULL,
    product_id  UUID        NOT NULL,
    url         TEXT        NOT NULL,
    alt_text    TEXT,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID,
    CONSTRAINT pk_catalog_product_image PRIMARY KEY (id),
    CONSTRAINT fk_catalog_product_image_product_id
        FOREIGN KEY (product_id) REFERENCES catalog_product (id) ON DELETE CASCADE
);
CREATE INDEX ix_catalog_product_image_product_id ON catalog_product_image (product_id);
