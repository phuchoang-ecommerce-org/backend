-- Sprint 05 (US-CUS-08) — a requested new email address is held pending until proven (UC-CUS-08
-- A1). The current `email` stays in force until the verification token for `pending_email` is
-- consumed, so the two columns are deliberately independent rather than one being derived from
-- the other's history.
ALTER TABLE identity_account ADD COLUMN pending_email TEXT;
