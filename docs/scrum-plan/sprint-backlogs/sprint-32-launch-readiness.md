<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 32 — Release 3: Reviews, Reporting & Launch Readiness

**Canonical sprint:** [Sprint 32 — Release 3: Reviews, Reporting & Launch Readiness](../../sprint-backlogs/sprint-32-launch-readiness.md)
**Lane:** Backend · R3 · **Gate:** **`RR` — Release Readiness Review** · **Backend 23 pts · Frontend 19 pts**

---

## Sprint Goal

> **The release is ready and its unverified claims are stated as unverified.**

The last sprint of thirty-six. Six stories close the backlog — all 87 user stories delivered — and then the sprint does the thing the whole document set has been building toward: it **completes the `AC-01`–`AC-06` table honestly**.

`AC-05` and `AC-06` are recorded as **unverified**, pending the load rig ([`Testing and Benchmark Strategy.md`](../../../SA-docs/01-system/Testing%20and%20Benchmark%20Strategy.md) §11, §7.9). IH-3 row 9 already established that; this sprint restates it in the release's own words rather than softening it on the way out. **A release is Done when the table is filled in truthfully, not when every row says met** ([`../definition-of-done.md`](../definition-of-done.md) §6) — and a delivery plan that quietly claims what the test strategy says is unverified is the failure mode `P15` describes, arriving through the last available door.

Two stories carry a rule worth naming. `US-REV-05` `E5`: **a moderation whose audit entry cannot be written is not applied** — nothing external has happened, so refusing is safe, and an unattributable suppression of a customer's words is precisely what `P17` forbids. And `US-NTF-04` `E1`: **transactional notifications cannot be disabled** (`BR-NTF-02`) — a customer who does not know their order shipped will contact Support, and one who does not know they were refunded may dispute the charge.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-REV-02` | Edit Own Review | 3 |
| BE | `US-REV-03` | Delete Own Review | 2 |
| BE | `US-REV-05` | Moderate Review | 5 |
| BE | `US-NTF-04` | Manage Notification Preferences | 3 |
| BE | `US-RPT-03` | View Customer Report | 5 |
| BE | `US-RPT-06` | Export Report | 5 |
| | | **Backend total** | **23** |

## Backend Lane

### `US-RPT-06` Export Report (5 pts) — `requestReportExport`, `getReportExport`, `downloadReportExport`
- [ ] **`E1` — export must never be a route to data the actor could not view.** Authority for the underlying report is checked first; otherwise export becomes a privilege-escalation path around every control in the specification (`BR-AUD-02`, `P16`)
- [ ] **`E2` — an export exceeding the permitted size is declined** and the actor asked to narrow the period or filters. An unbounded export is both an operational risk and a data-exfiltration risk
- [ ] **`E3` — where an export contains personal data, the audit entry records that personal data left the platform** (`UC-RPT-03` `E4`, `P17`)
- [ ] Asynchronous: request → poll → download, so a large export does not hold a request open
- [ ] The download link is single-use or short-lived and **scoped to the requesting actor** — an export URL that outlives its authorisation is the same escalation `E1` prevents, arriving later

### `US-REV-05` Moderate Review (5 pts) — `moderateReview`, `listReviews`, `getReview`, `removeReviewImage`
- [ ] `E1` — authority declined **and recorded**. Moderation is the authority to suppress a customer's published words and is restricted accordingly (`P16`)
- [ ] `E2` — an already-moderated review **presents the existing decision** rather than applying a second, so the trail records one decision per action
- [ ] `E3` — no reason supplied is declined. A removal without a recorded reason cannot be defended to the author, to Legal, or to an auditor (`BR-AUD-01`, `P17`)
- [ ] `E4` — an author deleting during moderation still has the decision **recorded against the deleted review**, so their conduct remains visible even though the content is gone
- [ ] **`E5` — a failed audit write means the moderation is not applied.** This follows `UC-INV-04` `E4`, **not** `UC-PAY-06` `E7` — nothing external has happened, so refusing is safe
- [ ] `E6` — a failed notification to the author lets the moderation **stand** and retries (`BR-NTF-01`). Content breaching policy is not restored because a message failed
- [ ] Rating summary recalculated through the Sprint 24 `EN-EVENT-6` projection, never synchronously

### `US-RPT-03` View Customer Report (5 pts) — `getCustomerReport`
- [ ] `E3` — a reporting outage reports the failure and **transactional operations are unaffected** (`UC-RPT-01` `E3`)
- [ ] **`E4` — customer-level export is permitted only where the role allows it, and is audited** (`UC-RPT-06`). Personal data leaving the platform is precisely the exposure `P16` and `P17` require to be traceable
- [ ] Reads the MongoDB read model only, per Sprint 26's `CON-06` rule; the as-at and staleness treatment of `UC-RPT-01` `E1`/`E2` applies unchanged

### `US-REV-02` Edit Own Review (3 pts) — `editOwnReview`
- [ ] **`E1` — a passed edit window declines, stating when it closed.** An unbounded edit window lets a favourable review be rewritten long after it has accrued visibility, which is a known abuse channel (`BR-REV-03`)
- [ ] `E2` — another customer's review is declined **and the attempt recorded** (`P16`)
- [ ] `E3` — a moderator-removed review is declined, so **a removed review cannot be edited back into visibility**
- [ ] **`E4` — a failed validation leaves the original unchanged.** A failed amendment never destroys the existing review
- [ ] Closes the offer `UC-REV-01` `E3` has made since Sprint 24

### `US-NTF-04` Manage Notification Preferences (3 pts) — `getOwnNotificationPreferences`, `setOwnNotificationPreferences`, `unsubscribeFromPromotionalNotifications`
- [ ] **`E1` — opting out of transactional notifications is declined.** Order, payment and shipment notifications about a customer's own transactions cannot be disabled (`BR-NTF-02`). The rule has been enforced since Sprint 23; this is the surface that explains it
- [ ] `E2` — an expired or consumed unsubscribe token declines and offers signing in. **The customer is never left with no route to opt out**
- [ ] **`E3` — a failed store changes nothing and says so.** A silent failure means unwanted mail continues while the customer believes it has stopped — a compliance exposure as much as an annoyance
- [ ] `E4` — a notification already in flight may still be delivered; preferences are evaluated when a notification is raised and **the platform does not claim retrospective effect**
- [ ] `E5` — another customer's preferences declined and recorded

### `US-REV-03` Delete Own Review (2 pts) — `deleteOwnReview`, `removeReviewImage`
- [ ] `E1` — another customer's review declined and recorded
- [ ] `E2` — an already-deleted review reports success; the goal already holds
- [ ] **`E3` — a failed aggregate recalculation still withdraws the review from display** and retries (`NFR-REL-04`). A briefly stale aggregate is preferable to a withdrawn review remaining visible

---

## Integration Risk & Dependencies


**There is no Contract Sync gate after this sprint.** `G15` was the last, and the Release Readiness Review states status rather than finding defects. Six stories on both lanes integrate with nothing scheduled behind them — so the integration has to happen *inside* the sprint, deliberately, not at a gate that does not exist.

Second, and the one this sprint most needs to resist: **the readiness review will be under pressure to look finished.** `AC-05` and `AC-06` will be sitting at unverified in the last sprint of a seventeen-month plan, and the cheapest available edit is to call them "in progress". IH-3 row 9 anticipated this; so does [`../definition-of-done.md`](../definition-of-done.md) §6. The correct outcome is a table with two honest failures in it.

Third: neither lane has reserve. If a story slips, the readiness review is what gets compressed — which is exactly backwards. **Timebox the review and protect it at Planning.**

---

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
