<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 23 — Notification

**Canonical sprint:** [Sprint 23 — Notification](../../sprint-backlogs/sprint-23-notification.md)
**Lane:** Backend · R1 · **Gate:** **`G11` — Contract Sync** · **Backend 21 pts · Frontend 13 pts**

---

## Sprint Goal

> **The platform tells people what happened.**

`notification` is a pure consumer — it subscribes to the Sprint 08 outbox's topics and owns no command path of its own. That shape decides its exception flows: **the business event it reports is never affected by a delivery failure.** `UC-NTF-01` `E7` and `UC-NTF-02` `E1` both say so explicitly, and `UC-ORD-05` `E9` said it first — an order is never reversed because its confirmation could not be composed.

The mirror rule is `BR-NTF-01`: a notification is **never marked delivered** when it was not. `E1` retries with backoff and then records **undeliverable**, surfaced operationally. This is `P6` in its most visible form — the customer who was never told.

This sprint also closes the escalation channel Sprint 20's `US-SHP-03` `E5` has been carrying without one.

**`R1` closes here.** Sprint 24 begins Release 2.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-NTF-01` | Deliver Email Notification | 8 |
| BE | `US-NTF-02` | Deliver In-App Notification | 5 |
| BE | `US-NTF-03` | View In-App Notifications | 3 |
| BE | `EN-BENCH-1` | `bench/smoke.js` — k6 scenarios S1–S5 reconciled against `openapi.yaml` | 5 |
| | | **Backend total** | **21** |

## Backend Lane

### `US-NTF-01` Deliver Email Notification (8 pts) — `listNotificationDeliveries`
- [ ] Kafka consumer over the order, payment and shipment topics; **idempotency and ordering guards reuse `EN-EVENT-2`'s**, not a fourth implementation
- [ ] **`E6` — a notification that cannot be recorded is not sent.** Recording precedes dispatch, so a message sent with no record of it is one nobody can later confirm was sent (`P6`)
- [ ] **`E4` — an event raised more than once sends one notification.** Duplicate confirmations for one order teach customers to distrust the channel — and the outbox is at-least-once by design, so duplicates are routine
- [ ] `E1` — a provider rejection records **failed** and retries with backoff; after the configured attempts it records **undeliverable** and surfaces it operationally. **It is never marked delivered** (`BR-NTF-01`)
- [ ] `E2` — an unreachable provider leaves it **pending** and retries. Because recording precedes dispatch, nothing is lost to a provider outage (`NFR-AVAIL-03`)
- [ ] `E3` — a bouncing address records undeliverable and **flags the address for correction**; repeated bounces suppress further sending, so provider reputation is not spent on a dead address
- [ ] `E5` — no registered address records undeliverable **with the reason**, not discarded: the absence is itself worth surfacing
- [ ] **`E7` — a composition failure is recorded and escalated, and the business event it reports is unaffected** (`UC-ORD-05` `E9`)
- [ ] This is the escalation channel Sprint 20's `US-SHP-03` `E5` has been carrying without one — wire it
- [ ] `BR-NTF-02` — transactional notifications are not suppressible by preference. `US-NTF-04` is Sprint 32; the **rule** is enforced here regardless

### `US-NTF-02` Deliver In-App Notification (5 pts) — internal consumer
- [ ] `E1` — a failed recording retries; **the business event is unaffected**
- [ ] `E2` — a duplicated event records once
- [ ] `E3` — a suspended or deleted account records **undeliverable with the reason**, not discarded
- [ ] **`E4` — a notification for another customer's account is refused and recorded. In-app notifications are scoped to their recipient absolutely** (`BR-AUD-02`, `P16`)

### `US-NTF-03` View In-App Notifications (3 pts) — `listOwnNotifications`, `setNotificationReadState`, `dismissNotification`, `markNotificationsRead`
- [ ] `E1` — no notifications returns an explicit empty page, never an error
- [ ] **`E2` — another customer's notifications are declined and the attempt recorded.** Notifications reveal order history and are scoped absolutely (`P16`)
- [ ] **`E3` — a failed read-state change still returns the notification content.** A read-state failure never withholds the message
- [ ] `E4` — a notification referencing a deleted order or product returns **its recorded text** (`FR-DAT-04`)
- [ ] Cursor pagination on the Sprint 02 envelope

### `EN-BENCH-1` `bench/smoke.js` — k6 scenarios S1–S5 (5 pts)
- [ ] Scenarios S1–S5 of [`Testing and Benchmark Strategy.md`](../../../SA-docs/01-system/Testing%20and%20Benchmark%20Strategy.md) written as k6, **reconciled against `openapi.yaml`** — a scenario hitting a path or payload the contract does not define is a defect in the scenario
- [ ] Smoke scale only. **The load rig `NFR-PERF` and `AC-05`/`AC-06` need is deliberately deferred** by §7.9 with five dated triggers; this item does not discharge that deferral and must not be reported as doing so
- [ ] Runnable against a local stack by one developer in one command
- [ ] Records what it measured and at what scale, so the numbers are not later read as capacity evidence

---

## Integration Risk & Dependencies


**`notification` has almost no contract surface** — two of its three stories are consumers with none at all — so `G11` verifies very little of what this sprint actually built. The guarantees that matter (recorded-before-sent, never-marked-delivered, duplicate-collapsed, business-event-unaffected) are verified by the sprint's own tests or not at all.

The concrete exposure: **`E4` duplicate collapse meets an at-least-once outbox.** If the idempotency key for a notification is derived from anything but the event id, a redelivery sends a second email — and the failure is invisible in every test that publishes each event once.

Second: three reporting screens are built against Prism for three sprints, and reporting is the domain where mock data is **least** representative — staleness, incompleteness and zero are the states that matter, and Prism generates none of them naturally. Exercise them by hand-editing mock responses.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
