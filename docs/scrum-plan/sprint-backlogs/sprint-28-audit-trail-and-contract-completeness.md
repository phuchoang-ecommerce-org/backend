<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 28 — Audit Trail & Contract Completeness

**Canonical sprint:** [Sprint 28 — Audit Trail & Contract Completeness](../../sprint-backlogs/sprint-28-audit-trail-and-contract-completeness.md)
**Lane:** Backend · R2 · **Gate:** none · **Backend 18 pts · Frontend 6 pts**

---

## Sprint Goal

> **The audit trail is searchable and append-only.**

The trail has been *written* since Sprint 12. Reading it is its own story, and `UC-AUD-01` `E3` fixes its shape: **amendment or deletion is refused for every role including `ADMINISTRATOR`, and the attempt is itself recorded as a security event.** An audit trail a sufficiently privileged actor can edit provides no assurance at all, which is why `BR-AUD-01` admits no exception.

So the deliverable is partly an absence. **No edit or delete control exists to draw — in the UI or the data model.** Not a hidden button, not an endpoint guarded by a role check. The capability does not exist.

`EN-CONTRACT-3` at 13 points is the larger item and the one that closes a promise made at every gate since `G0`. **The permission-matrix test is generated from spec × matrix across all 155 operations** — replacing eleven gates' worth of hand-walked check 7 and IH-1 row 9's recorded-coverage caveat with a suite. Testing Strategy §10's third coverage rule says why: *an uncovered cell is an unverified authorisation decision.*

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-AUD-02` | Search Audit Trail | 5 |
| BE | `EN-CONTRACT-3` | Permission-matrix test generated from spec × matrix, all 155 operations | 13 |
| | | **Backend total** | **18** |

## Backend Lane

### `EN-CONTRACT-3` Permission-matrix test generated from spec × matrix (13 pts)
- [ ] **Generated from the cross product of `openapi.yaml`'s 155 operations and [`Permission Matrix.md`](../../../SA-docs/04-shared/Permission%20Matrix.md)** — not hand-written. A hand-written matrix test is a matrix test that drifts from the matrix
- [ ] For every cell: a role the matrix **grants** succeeds; a role it does **not** grant is **refused by the server**. Both halves, every cell — testing only the grants leaves every bypass undetected
- [ ] **An operation with no matrix row fails the build**, and a matrix row naming no operation fails it too. The two documents are checked against each other, not merely used together
- [ ] Ownership-scoped refusals return **the same response as data that does not exist** (`UC-AUD-03` `E2`), so identifiers cannot be probed. `404`, never `403`, and the test asserts the status code
- [ ] `UC-AUD-03` `E7` — an **indeterminate** authorisation is refused. The safe default is denial, and a test that only exercises reachable decisions never reaches this branch
- [ ] Closes what [`ADR-0018`](../../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md) §5 concedes ArchUnit cannot do: *calling* `AuthorizationService` is structurally checkable, calling it **with the right permission** is not — and [`Security.md`](../../../SA-docs/01-system/Security.md) §12.1 makes that a test concern. This is that test
- [ ] Joins `EN-CI-1`'s stage 4; non-skippable, and within the stage's budget
- [ ] **This supersedes the manual check 7 at every future gate.** Record that, so `G14` and `G15` cite the suite rather than re-walking by hand

### `US-AUD-02` Search Audit Trail (5 pts) — `searchAuditTrail`, `getAuditEntry`
- [ ] **The read path only.** No write, amend, or delete operation exists in `openapi.yaml` for audit entries, and `EN-CONTRACT-2`'s code→spec direction confirms none exists in the code either
- [ ] `E1` — authority declined **and the attempt recorded**. The trail describes who exercised which authority and is itself sensitive (`P16`)
- [ ] **`E2` — an empty result is presented explicitly, distinguished from a failed search.** "No such action was recorded" and "the search did not run" are very different findings in an investigation
- [ ] **`E3` — an impractically large range asks the actor to narrow it.** The platform does not return a truncated set that could be mistaken for a complete one
- [ ] **`E4` — entries beyond the retention horizon are stated as unavailable**, rather than silently returning a partial record that reads as complete
- [ ] `E5` — amendment attempted from this view is refused for every role and recorded (`UC-AUD-01` `E3`). **Refused because the capability does not exist**, not because a role check failed
- [ ] The append-only property is enforced at the **schema level** — no update or delete grant on the table — so it survives an application-layer mistake
- [ ] Cursor pagination on the Sprint 02 envelope; filters by actor, action, target and period

---

## Integration Risk & Dependencies


**`EN-CONTRACT-3` is the highest-yield item in the block and it will find things.** Eleven gates have walked permission-matrix cells by hand, IH-1 row 9 recorded explicitly that coverage was *"whatever was walked by hand"*, and 155 operations across a dozen roles is far more cells than any of those walks covered. Findings here are authorisation defects, not test defects, and each needs triage inside the sprint rather than a carry-forward — an unverified authorisation decision is the one class of finding that should not wait.

Budget deliberately for it. If the item runs long, the 12-point frontend reserve exists for exactly this.

Second: **no gate closes this sprint**, so `US-AUD-02` moves from mock to real without one. `/admin/audit` has been mock-served since Sprint 25 and integrates at `G14` next sprint — `E2`, `E3` and `E4` all being states Prism never generated.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
