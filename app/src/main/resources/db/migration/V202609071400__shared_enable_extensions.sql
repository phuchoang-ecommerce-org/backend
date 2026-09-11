-- Extensions and the application database role every later migration assumes.
-- ecp_app is the role V202609071455__audit_create_audit_entry.sql grants
-- INSERT/SELECT and revokes UPDATE/DELETE/TRUNCATE from (ADR-0017, ADR-0029 §4)
-- — it must exist before that script runs, so it is created first, here.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE ROLE ecp_app;
