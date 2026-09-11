-- Database.md §4.10 — notification_request, notification_preference. BR-NTF-01/02.
CREATE TABLE notification_request (
    id                    UUID        NOT NULL,
    recipient_account_id  UUID,
    recipient_address     TEXT        NOT NULL,
    channel               VARCHAR(16) NOT NULL,
    category              VARCHAR(16) NOT NULL,
    template_code         VARCHAR(64) NOT NULL,
    triggering_event_id   UUID        NOT NULL,
    triggering_event_type VARCHAR(64) NOT NULL,
    payload               JSONB       NOT NULL DEFAULT '{}'::jsonb,
    status                VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempt_count         INTEGER     NOT NULL DEFAULT 0,
    last_error            TEXT,
    dispatched_at         TIMESTAMPTZ,
    version               BIGINT      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by            UUID,
    CONSTRAINT pk_notification_request PRIMARY KEY (id),
    -- BR-NTF-01: delivered at least once or recorded undeliverable, never
    -- silently dropped. One request per (event, recipient, channel) makes
    -- redelivery of the source event idempotent here.
    CONSTRAINT ux_notification_request_event_recipient_channel
        UNIQUE (triggering_event_id, recipient_address, channel),
    CONSTRAINT ck_notification_request_channel CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP')),
    -- BR-NTF-02: opt-out applies to promotional, never transactional.
    CONSTRAINT ck_notification_request_category
        CHECK (category IN ('TRANSACTIONAL', 'PROMOTIONAL')),
    CONSTRAINT ck_notification_request_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'UNDELIVERABLE', 'SUPPRESSED'))
);
CREATE INDEX ix_notification_request_pending
    ON notification_request (created_at) WHERE status = 'PENDING';
CREATE INDEX ix_notification_request_recipient
    ON notification_request (recipient_account_id, created_at DESC);

CREATE TABLE notification_preference (
    id                  UUID        NOT NULL,
    account_id          UUID        NOT NULL,
    channel             VARCHAR(16) NOT NULL,
    promotional_opt_in  BOOLEAN     NOT NULL DEFAULT false,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    CONSTRAINT pk_notification_preference PRIMARY KEY (id),
    CONSTRAINT ux_notification_preference_account_channel UNIQUE (account_id, channel),
    CONSTRAINT ck_notification_preference_channel
        CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP'))
);
