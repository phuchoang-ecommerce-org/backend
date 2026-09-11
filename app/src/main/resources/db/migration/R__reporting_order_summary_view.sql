-- Database.md §4.12 — repeatable, re-runs on checksum change (ADR-0029 §4:
-- repeatable migrations only for replaceable objects). This view reads
-- ordering_order across the module boundary — the one sanctioned exception in
-- the schema (ADR-0009 §5): read-only, non-authoritative, zero upstream
-- influence. Not the dashboard path — NFR-PERF-05 routes dashboards to MongoDB
-- (§7.2) instead.
CREATE OR REPLACE VIEW reporting_order_summary AS
SELECT date_trunc('day', o.paid_at) AS day,
       o.currency,
       count(*)                     AS order_count,
       sum(o.total_amount)          AS gross_amount,
       sum(o.discount_amount)       AS discount_amount
FROM   ordering_order o
-- BR-RPT-01: revenue counts only Paid-or-beyond orders. Refunds and returns are
-- excluded from the period in which the order was placed and recognised in the
-- period they occur — which is why REFUNDED and RETURNED are absent here and
-- accounted separately in the MongoDB projection (§7.2).
WHERE  o.status IN ('PAID', 'PROCESSING', 'PACKED', 'SHIPPING',
                    'DELIVERED', 'COMPLETED')
GROUP BY 1, 2;
