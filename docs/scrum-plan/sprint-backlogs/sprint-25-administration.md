<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 25 — Administration: Accounts, Roles & Bulk Actions

**Canonical sprint:** [Sprint 25 — Administration: Accounts, Roles & Bulk Actions](../../sprint-backlogs/sprint-25-administration.md)
**Lane:** Backend · R2 · **Gate:** **`G12` — Contract Sync** · **Backend 20 pts · Frontend 13 pts**

---

## Sprint Goal

> **Operators can run the business.**

`US-ADM-06` is the most consequential story in the plan by authority, not by points. **`E1` — an actor granting themselves a role they do not hold is refused and recorded as a security event**, because self-elevation would make every other access control in the system decorative. **`E2` — revoking the last `ADMINISTRATOR` is refused**, because a platform with no Administrator cannot be administered, including to undo that change. Both are `BR-AUD-03`.

`EN-CONTRACT-2` closes the second contract direction. Since Sprint 16 the spec→code direction has been automated and code→spec has been a manual confirmation at every gate. From this sprint on, **an undocumented endpoint fails the build** — which means `G12` is the first gate whose check 3 is machine-verified rather than asserted.

The frontend runs its Playwright suite against real endpoints for the first time.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-ADM-03` | Manage Customer Accounts | 5 |
| BE | `US-ADM-04` | Manage Orders | 5 |
| BE | `US-ADM-06` | Manage User Roles | 5 |
| BE | `EN-CONTRACT-2` | Contract test code→spec; undocumented endpoint fails the build | 5 |
| | | **Backend total** | **20** |

## Backend Lane

### `US-ADM-06` Manage User Roles (5 pts) — `listRoles`, `listAccountRoles`, `grantAccountRole`, `revokeAccountRole`
- [ ] **`E1` — self-elevation is refused and recorded as a security event.** An actor may not grant themselves a role they do not hold (`BR-AUD-03`, `P16`) — enforced in the domain, not at the controller, so no entry point can bypass it
- [ ] **`E2` — revoking the last `ADMINISTRATOR` is refused.** Assert it under concurrency too: two simultaneous revocations of the last two administrators must not both succeed
- [ ] `E3` — a non-`ADMINISTRATOR` is refused **and recorded**. Role management is the most consequential authority in the platform
- [ ] `E4` — no reason supplied is refused. "Who granted this and why" is the first thing an auditor asks (`BR-AUD-01`, `P17`)
- [ ] **`E5` — a failed audit write means the change is not applied.** The `UC-AUD-01` `E1` branch — and `UC-AUD-01` `E1` names role change explicitly as one of its cases
- [ ] `E6` — a revocation proceeds even where the user has outstanding work; **authority is not extended for convenience**
- [ ] Role changes take effect within one refresh interval (`UC-AUD-03` `E5`), and an urgent revocation ends sessions at once — the `endAccountSessions` path below

### `US-ADM-03` Manage Customer Accounts (5 pts) — `searchAccounts`, `getAccount`, `correctAccountProfile`, `setAccountStatus`, `closeAccount`, `endAccountSessions`
- [ ] `E1` — authority refused **and recorded**. This is `P16`'s "a support agent who can access data outside their responsibility"
- [ ] **`E2` — credentials are never disclosed to any role.** Passwords exist only as one-way hashes and are not retrievable (`NFR-SEC-02`, `NFR-SEC-07`) — assert that no response, log line, or error message can carry one
- [ ] `E3` — a suspension without a recorded reason is refused (`BR-AUD-01`)
- [ ] **`E4` — suspending an account blocks access, but open orders continue to be fulfilled or are resolved deliberately.** The platform does not abandon a paid order because an account was suspended (`P7`)
- [ ] `E5` — a failed audit write means the change is not applied
- [ ] `endAccountSessions` invalidates every outstanding `REFRESH` token for the account, the same mechanism Sprint 05's `changeOwnPassword` established — not a second implementation
- [ ] Cursor pagination on `searchAccounts`; permission-matrix cell asserted per operation

### `US-ADM-04` Manage Orders (5 pts) — `listOrders` (scoped), `advanceOrderStatusesInBulk`
- [ ] **`E1` — an illegal transition is refused for every role including `ADMINISTRATOR`**, stating the current state and available transitions. `ECP-ORD-4091`. Goes through the Sprint 19 state machine; **no admin-only transition path exists** (`BR-ORD-01`, `P5`)
- [ ] `E2` — viewing, cancelling and refunding an order are **separate authorities** (`P16`); each cell asserted
- [ ] **`E3` — amending a placed order's contents or total is refused.** Once Paid, commercial terms are fixed; the remedies are cancellation, return and refund, each leaving a record (`BR-ORD-06`, `P17`)
- [ ] `E4` — an order that changed while being inspected re-evaluates and refuses as `E1` if no longer legal
- [ ] `E5` — a failed audit write means the action is not applied, **except where it has already moved money externally** (`UC-PAY-06` `E7`) — the one place in this story where the `E2` branch applies instead
- [ ] `advanceOrderStatusesInBulk` reports **per-order** outcomes; an aggregate result hides the `E1` failures inside it
- [ ] `listOrders` scoped by role: the admin form and the Sprint 05 customer-scoped form are one operation with different authority, and the customer-scoped behaviour must not regress

### `EN-CONTRACT-2` Contract test code→spec (5 pts)
- [ ] Every route the application exposes is checked against `openapi.yaml`. **An undocumented endpoint fails the build** — no `TODO`, no allow-list of known exceptions
- [ ] Internal ports must produce **no** endpoint: `StockReservationPort` (Sprint 11) and `PromotionRedemptionPort` (Sprint 15) have been confirmed by hand at three gates; this automates that confirmation permanently
- [ ] Management-port and actuator routes are excluded **explicitly and by name**, not by a pattern that would also hide a real leak
- [ ] Non-skippable per [`ADR-0018`](../../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md), and fast enough to stay in the default `check` task
- [ ] Planted-violation demonstration: add an undocumented endpoint, watch the build fail, remove it, watch it pass
- [ ] **`EN-CONTRACT-3` (Sprint 28)** generates the permission-matrix test across all 155 operations — this item does not cover authorisation, only existence and shape

---

## Integration Risk & Dependencies


**`G12` is the first gate whose check 3 is machine-verified**, and `EN-CONTRACT-2` may find undocumented endpoints that eleven gates of manual confirmation missed. Budget for that inside the sprint: each finding is either an `openapi.yaml` amendment or a route that should not exist, decided case by case, and each is a drift log entry per [`ADR-0031`](../../../SA-docs/01-system/ADR/ADR-0031-contract-first-openapi.md).

Second: **Sprint 24's review operations have no gate of their own.** They must be added to `G12`'s scope explicitly — the increment is nominally administration, and `review` would otherwise pass from mock to real without any gate looking at it.

Third: `EN-FE-E2E-1`'s first run against real endpoints is where accumulated mock assumptions surface all at once. Treat early failures as findings about the assumptions, not about the suite.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
