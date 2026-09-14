<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — IH-1 — Integration Hardening: Session & Catalog

**Canonical sprint:** [IH-1 — Integration Hardening: Session & Catalog](../../sprint-backlogs/ih-1-session-and-catalog.md)
**Lane:** Backend · R1 · **Position:** after Sprint 09, before Sprint 10 · **No new stories · no story points**

---

## Goal

> **Session and catalog, end to end.**

This is not a Contract Sync gate and it is not a catch-up sprint. A gate checks the increment just delivered; IH-1 checks the **properties that no single increment owns** — session custody, boundary posture, the invalidation chain, and one correlation id running the length of it.

IH-1 sits first of the three hardening sprints because **session custody is the hardest thing in the plan to retrofit**. Every later sprint assumes it; if it is wrong, it is wrong everywhere at once.

Both developers, both lanes, full sprint. **No new stories are committed and no points are carried** — an IH sprint that takes on delivery work is an IH sprint that reports green because it ran out of time to look.

---

## Backend Verification Checklist

- [ ] Confirm no Server Action or route handler echoes the token into a response body or an error message
- [ ] Drive N concurrent requests through an expired access token and assert **exactly one** refresh call reaches `ecp-api`
- [ ] Sign in as a `CUSTOMER`, navigate to `/admin` and to three `(admin)` detail routes. **The shell renders**, its sections are empty, and every read behind it returned `403` **from the server**
- [ ] Tamper with the callback signature and confirm refusal; confirm the refusal is visible in logs rather than silent
- [ ] Issue one browser request that causes a catalog write, and follow **one** id through the API log, the outbox row, the Kafka envelope, the consumer, and the revalidation callback
- [ ] Walk every identity and catalog operation delivered through Sprint 09 against its matrix row: each role that is granted succeeds, each role that is not is **refused by the server**
- [ ] Hiding a control in the UI counts for nothing here. The check is the API response
- [ ] `UC-AUD-01` remains the Sprint 03/04 stub listener. The **refusal path** (`E1`: audit fails → the change is not applied) is what row 9's adjacent behaviour depends on; confirm the refusal is real even though the persistence is not (`US-AUD-01`, Sprint 12)

## Cross-Lane Milestones

- Complete the shared hardening exit criterion with the other lane.
- Record any unresolved finding as a sized canonical backlog item with a named sprint.
- See the [canonical hardening backlog](../../sprint-backlogs/ih-1-session-and-catalog.md) for the full system checklist.

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
