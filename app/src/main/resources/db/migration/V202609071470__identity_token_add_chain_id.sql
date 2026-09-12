-- Sprint 04 (US-CUS-05) — refresh-token rotation chains (ADR-0016 §4). `chain_id` links every
-- token produced by rotating one login's refresh token together; a REFRESH row's own id is its
-- chain's head. Reuse of an already-consumed REFRESH token invalidates every row sharing its
-- chain_id, not just that one row. NULL for EMAIL_VERIFICATION/PASSWORD_RESET rows, which never
-- rotate.
ALTER TABLE identity_token ADD COLUMN chain_id UUID;

UPDATE identity_token SET chain_id = id WHERE token_type = 'REFRESH';

CREATE INDEX ix_identity_token_chain_id ON identity_token (chain_id) WHERE token_type = 'REFRESH';
