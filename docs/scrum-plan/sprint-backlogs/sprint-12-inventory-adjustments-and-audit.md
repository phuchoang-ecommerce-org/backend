<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 12 — Inventory: Adjustments, Levels & Audit Entries

**Canonical sprint:** [Sprint 12 — Inventory: Adjustments, Levels & Audit Entries](../../sprint-backlogs/sprint-12-inventory-adjustments-and-audit.md)
**Lane:** Backend · R1 · **Gate:** none · **Backend 21 pts · Frontend 13 pts**

---

## Sprint Goal

> **Stock is adjustable and every command is audited.**

`US-AUD-01` lands here and not earlier for a stated reason: `UC-AUD-01` is worth building once there are commands worth auditing, and inventory adjustment is the first one that is a **direct financial control** (`P16`, `P17`). The audit stub that Sprints 03, 04, 05 and 09 have been logging through is replaced by real `audit_entry` persistence this sprint — the table has existed since Sprint 03, ahead of its code.

The hard part is not writing entries. It is `E4`: **an adjustment whose audit entry cannot be written is not applied.** Every command path delivered so far has to adopt that refusal, which is why this is an 8-point cross-cutting story rather than a 3-point table.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-INV-04` | Adjust Inventory | 5 |
| BE | `US-INV-05` | View Inventory Levels | 5 |
| BE | `US-ADM-05` | Manage Inventory Adjustments | 3 |
| BE | `US-AUD-01` | Record Audit Entry | 8 |
| | | **Backend total** | **21** |

## Backend Lane

### `US-AUD-01` Record Audit Entry (8 pts) — internal, every command path
- [ ] Real `audit_entry` persistence replacing the Sprint 03/04 `NotificationAndAuditStubListeners` stub; the table already exists from Sprint 03
- [ ] **`E1` — entry cannot be written and the action is reversible → the action is not applied.** This is the default and it is retrofitted onto *every* command path delivered so far: `US-CUS-08` profile updates (Sprint 05), all thirteen catalog writes (Sprint 09), and the adjustments in this sprint
- [ ] **`E2` — entry cannot be written and the effect is irreversible → the action stands, and the gap is escalated.** No such path exists yet (`UC-PAY-06` is Sprint 22, `UC-PRM-05` is Sprint 16) — build the distinction now so those sprints select a branch rather than invent one
- [ ] `E3` — amendment or deletion of an entry is **refused for every role including `ADMINISTRATOR`**, and the attempt is itself recorded as a security event (`BR-AUD-01`, `NFR-OBS-02`). Enforced at the schema and the port, not by the absence of an endpoint
- [ ] `E4` — an action with no identifiable actor is written **attributed to the originating process and flagged**, never left unrecorded
- [ ] `E5` — retention per **[A-13]** / `FR-DAT-05`; expiry is by policy and is itself recorded
- [ ] `audit`'s `allowedDependencies` and ArchUnit rules confirm no module reads another's entries directly
- [ ] Migrate the Sprint 03/04/05/09 stub call sites over and **delete the stub** — a stub left in place beside the real implementation is the next sprint's ambiguity

### `US-INV-04` Adjust Inventory (5 pts) — `adjustStock`, `listStockAdjustments`
- [ ] `E1` — a negative adjustment below reserved stock is **declined**, reporting how many units are reserved and against which orders. `ECP-INV-4091`. The record must never say the business holds less than it has already promised (`BR-INV-01`)
- [ ] `E2` — no reason supplied is declined (`BR-INV-03`). An adjustment that cannot answer "who, when, why" is what `P17` forbids
- [ ] `E3` — actor lacking authority is declined **and the attempt recorded** (`P16`)
- [ ] `E4` — audit write failure means the adjustment is **not applied**, and the whole thing is retried. Uses the `US-AUD-01` `E1` path above, not a local copy
- [ ] `E5` — concurrent adjustments to one SKU **both apply**, each computed against the value current when applied, both appearing separately in the trail. L5 concurrency test, same rig as Sprint 11's race

### `US-INV-05` View Inventory Levels (5 pts) — `listStockItems`, `getStockItem`, `listWarehouses`
- [ ] `E1` — quantities and warehouse structure are **never** exposed to `CUSTOMER` or `GUEST` (`BR-AUD-02`); permission-matrix cell asserted per operation
- [ ] `E2` — an unknown SKU reports unknown **without disclosing whether it once existed** — the same non-disclosure rule as Sprint 05's `getOwnAddress`
- [ ] `E3` — figures may briefly lag reservations in flight, and that is acceptable (`NFR-PERF-06`) because no decision here consumes stock
- [ ] Cursor pagination on the Sprint 02 envelope

### `US-ADM-05` Manage Inventory Adjustments (3 pts) — `listStockAdjustments`
- [ ] The operator-facing view of the adjustment history; `E1`–`E4` mirror `UC-INV-04`'s and must not diverge from them
- [ ] `E3` — authority declined **and recorded**; inventory adjustment is a standing internal-fraud risk (`P16`, `P17`)

---

## Integration Risk & Dependencies


**`US-AUD-01` changes the behaviour of code that already passed a gate.** Every command path from Sprints 03, 05 and 09 acquires a new refusal branch this sprint. Nothing in the `G6` checklist will look at Sprint 09's catalog writes again, so the regression has to be caught here — re-run the catalog write paths with audit persistence failing, and confirm the change is refused rather than silently applied.

Second: `adjustStock`'s `ECP-INV-4091` and the storefront's are now genuinely different screens for one code. Confirm the error map routes by **context**, not by code alone.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
