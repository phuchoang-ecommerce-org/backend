<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Release Plan — Enterprise Commerce Platform (ECP)

**Document type:** Derived lane release plan · **Audience:** Backend Engineering, Product Management
**Canonical source:** [PM release plan](../release-plan.md)

---

## Purpose

This view retains the canonical release order and all sprint positions, while focusing commitment detail on the backend lane. Each sprint links to a filtered task backlog; cross-lane work remains a delivery milestone rather than a duplicate worklist.

The plan remains **36 two-week sprints across R1–R3**, with the Sprint 29 Must cut line and the three Integration Hardening sprints unchanged.

## Sprint Map

| Sprint | BE pts | Shared gate or milestone | Sprint goal |
|---|---:|---|---|
| Sprint 00 — Foundation — Build & Toolchain | 14 | No Contract Sync | Both lanes have a build that enforces its own rules. |
| Sprint 01 — Foundation — Architecture Gate & Contract Harness  ▸ **G0** | 20 | G0 | The architecture gate is real and the contract harness runs. |
| Sprint 02 — Wire Format & Application Shell | 20 | No Contract Sync | Every response shape a controller will ever return is decided once. |
| Sprint 03 — Identity — Registration & Sign-in  ▸ **G1** | 20 | G1 | A customer can register, verify, sign in, and sign out. |
| Sprint 04 — Identity — Session, RBAC & Rate Limit | 20 | No Contract Sync | Authorisation and rate limiting hold on every path. |
| Sprint 05 — Identity — Account, Addresses & Recovery  ▸ **G2** | 19 | G2 | A customer owns their account. |
| Sprint 06 — Catalog — Categories, Listings & Variants | 21 | No Contract Sync | The catalog is browsable. |
| Sprint 07 — Catalog — Product Detail & Read Cache  ▸ **G3** | 18 | G3 | The product page is the reference implementation of `NFR-AVAIL-02`. |
| Sprint 08 — Event Backbone — Outbox & Kafka | 21 | No Contract Sync | No accepted business event can be silently lost. |
| Sprint 09 — Catalog Administration  ▸ **G4** | 21 | G4 | An operator can manage the catalog, and the storefront notices. |
| ⛓ IH-1 — Session & Catalog | 0 | Integration Hardening | Session and catalog, end to end. |
| Sprint 10 — Search — Keyword, Facets & Projection | 21 | No Contract Sync | Search answers from its own read store. |
| Sprint 11 — Inventory — The Reservation Model  ▸ **G5** | 18 | G5 | The oversell guarantee is proved, not asserted. |
| Sprint 12 — Inventory — Adjustments, Levels & Audit Entries | 21 | No Contract Sync | Stock is adjustable and every command is audited. |
| Sprint 13 — Cart — Lines & Guest Cart  ▸ **G6** | 20 | G6 | A guest can build a cart. |
| Sprint 14 — Cart — Merge, Expiry & Checkout Funnel (FE) | 21 | No Contract Sync | A guest cart survives sign-in, and an abandoned one expires. |
| Sprint 15 — Promotion — Vouchers & Redemption  ▸ **G7** | 21 | G7 | A voucher is validated and redeemed without over-redemption. |
| Sprint 16 — Promotion — Flash Sale & Lifecycle | 21 | No Contract Sync | Promotions have a lifecycle. |
| Sprint 17 — Ordering — Checkout Funnel  ▸ **G8** | 18 | G8 | Checkout collects everything an order needs. |
| Sprint 18 — Ordering — Place Order (the Partnership) | 18 | No Contract Sync | Placing an order is atomic across three modules. |
| Sprint 19 — Ordering — Lifecycle & Admin Orders  ▸ **G9** | 19 | G9 | An order has a life after placement. |
| Sprint 20 — Shipping — Quotes, Shipments & Delivery | 21 | No Contract Sync | Goods move and the customer can see it. |
| ⛓ IH-2 — The Money Path | 0 | Integration Hardening | The money path — the sprint that decides whether `P6`, `P7` and `P8` are solved. |
| Sprint 21 — Payment — Authorise & Provider Callback  ▸ **G10** | 19 | G10 | Money can be taken. |
| Sprint 22 — Payment — Settle, Retry & Refund | 20 | No Contract Sync | Money can be settled, retried, and given back. |
| Sprint 23 — Notification  ▸ **G11** | 21 | G11 | The platform tells people what happened. |
| Sprint 24 — Review | 18 | No Contract Sync | Customers can review what they bought. |
| Sprint 25 — Administration — Accounts, Roles & Bulk Actions  ▸ **G12** | 20 | G12 | Operators can run the business. |
| Sprint 26 — Reporting — Read Models & Core Reports | 18 | No Contract Sync | Reporting answers from MongoDB, never from the write model. |
| Sprint 27 — Reporting — Inventory Report, Lag & CI  ▸ **G13** | 21 | G13 | Every reporting screen shows its own lag. |
| Sprint 28 — Audit Trail & Contract Completeness | 18 | No Contract Sync | The audit trail is searchable and append-only. |
| Sprint 29 — Release 2 Stabilisation  ▸ **G14** | 21 | G14 | Release 2 is stabilised and the event backbone is proved under failure. |
| ⛓ IH-3 — Whole System | 0 | Integration Hardening | The whole system, and an honest statement of what is still unverified. |
| Sprint 30 — Release 3 — Discovery Should/Could Stories | 18 | No Contract Sync | Discovery gets its `Should` and `Could` capability. |
| Sprint 31 — Release 3 — Cart, Orders & Personalisation  ▸ **G15** | 21 | G15 | Wishlist, returns, delivery estimates, and personalisation. |
| Sprint 32 — Release 3 — Reviews, Reporting & Launch Readiness  ▸ **RR** | 23 | No Contract Sync | The release is ready and its unverified claims are stated as unverified. |

## Working Commitments

- Follow the [shared Scrum framework](../scrum-framework.md); the two-lane board and the Integrated column still apply.
- Use the [backend product backlog](./product-backlog.md) for scheduled slices and [sprint backlogs](./sprint-backlogs/README.md) for task-level work.
- At every Contract Sync, verify the increment with the [shared integration checklist](../integration-plan.md#3-the-contract-sync-gate).
- During IH-1, IH-2, and IH-3, execute only the lane-owned verification checklist; do not pull forward story work.

## Canonical Detail

The [integrated release plan](../release-plan.md) remains authoritative for full two-lane point loads, sequencing rationale, capacity reserve, risks, and any conflict resolution.
