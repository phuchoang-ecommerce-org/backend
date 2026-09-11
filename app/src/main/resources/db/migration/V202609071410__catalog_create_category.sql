-- Database.md §4.2 — catalog_category. BR-CAT-03 (not its own ancestor).
CREATE TABLE catalog_category (
    id          UUID        NOT NULL,
    parent_id   UUID,
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL,
    path        TEXT        NOT NULL,
    depth       INTEGER     NOT NULL DEFAULT 0,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID,
    CONSTRAINT pk_catalog_category PRIMARY KEY (id),
    CONSTRAINT fk_catalog_category_parent_id
        FOREIGN KEY (parent_id) REFERENCES catalog_category (id),
    CONSTRAINT ux_catalog_category_slug UNIQUE (slug),
    CONSTRAINT ck_catalog_category_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_catalog_category_depth CHECK (depth >= 0)
);
CREATE INDEX ix_catalog_category_parent_id ON catalog_category (parent_id);
-- BR-CAT-03: a category may not be its own ancestor. `path` is the materialised
-- ancestor chain ('/root/apparel/shirts/'); the pre-check is
-- `NOT new_path LIKE old_path || '%'`, and the index makes both that check and
-- subtree reads a prefix scan rather than a recursive walk.
CREATE INDEX ix_catalog_category_path ON catalog_category (path text_pattern_ops);
