-- Database.md §4.9 — review_review, review_image (child). BR-REV-02/03/04.
CREATE TABLE review_review (
    id                UUID        NOT NULL,
    product_id        UUID        NOT NULL,
    customer_id       UUID        NOT NULL,
    order_id          UUID,
    rating            INTEGER     NOT NULL,
    title             TEXT,
    body              TEXT,
    moderation_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    moderated_by      UUID,
    moderated_at      TIMESTAMPTZ,
    moderation_reason TEXT,
    editable_until    TIMESTAMPTZ,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    CONSTRAINT pk_review_review PRIMARY KEY (id),
    -- BR-REV-02: at most one review per customer per product.
    CONSTRAINT ux_review_review_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT ck_review_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_review_review_moderation_status
        CHECK (moderation_status IN ('PENDING', 'PUBLISHED', 'REJECTED', 'HIDDEN')),
    CONSTRAINT ck_review_review_moderated
        CHECK (moderation_status = 'PENDING' OR moderated_at IS NOT NULL)
);
CREATE INDEX ix_review_review_product_published
    ON review_review (product_id, created_at DESC)
    WHERE moderation_status = 'PUBLISHED';
CREATE INDEX ix_review_review_pending
    ON review_review (created_at) WHERE moderation_status = 'PENDING';

CREATE TABLE review_image (
    id          UUID        NOT NULL,
    review_id   UUID        NOT NULL,
    url         TEXT        NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    size_bytes  INTEGER     NOT NULL,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    CONSTRAINT pk_review_image PRIMARY KEY (id),
    CONSTRAINT fk_review_image_review_id
        FOREIGN KEY (review_id) REFERENCES review_review (id) ON DELETE CASCADE,
    CONSTRAINT ck_review_image_size CHECK (size_bytes > 0)
);
CREATE INDEX ix_review_image_review_id ON review_image (review_id);
