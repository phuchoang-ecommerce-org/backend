-- Sprint 03 gap fix: V202609071406 created identity_role's shape (and the CHECK
-- constraint fixing its vocabulary) but never seeded the six rows Permission
-- Matrix.md §2.1 / Solution Architecture.md §4 name — identity_account_role's
-- FK to identity_role(id) has nothing to reference without this. Application
-- code resolves a role by `code`, never by this migration's generated id, so
-- the actual UUID values are not a contract.
INSERT INTO identity_role (id, code, description) VALUES
    (gen_random_uuid(), 'GUEST', 'An unauthenticated caller.'),
    (gen_random_uuid(), 'CUSTOMER', 'A registered account placing and managing orders.'),
    (gen_random_uuid(), 'STAFF', 'General staff member.'),
    (gen_random_uuid(), 'WAREHOUSE_OPERATOR', 'Fulfils and adjusts inventory.'),
    (gen_random_uuid(), 'CUSTOMER_SUPPORT', 'Assists customers; read access to accounts and orders.'),
    (gen_random_uuid(), 'ADMINISTRATOR', 'Full operator authority.');
