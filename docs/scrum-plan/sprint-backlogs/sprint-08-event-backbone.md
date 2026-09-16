<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 08 — Event Backbone: Outbox & Kafka

**Canonical sprint:** [Sprint 08 — Event Backbone: Outbox & Kafka](../../sprint-backlogs/sprint-08-event-backbone.md)
**Lane:** Backend · R1 · **Gate:** none · **Backend 21 pts · Frontend 13 pts**

---

## Sprint Goal

> **No accepted business event can be silently lost.**

`EN-EVENT-1` is the single largest enabler in the plan and the one everything downstream assumes: search projection (S10), catalog revalidation (S09), order lifecycle (S18), notification (S23), and the reporting read models (S26–S27) all take it as given. It is committed as one 21-point item rather than split, because an outbox that is half-built is an outbox that loses events — and the point of the sprint is the guarantee, not the table.

The frontend lane spends the sprint **a full sprint ahead of the backend**, building the admin catalog console against the Prism mock. That is the contract-first dividend working as designed, not slack.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `EN-EVENT-1` | Transactional outbox table + polling relay + event envelope + topic catalogue | 21 |
| | | **Backend total** | **21** |

## Backend Lane

### `EN-EVENT-1` Transactional outbox + relay + envelope + topic catalogue (21 pts)

**The outbox table and the write in the same transaction**
- [ ] Flyway migration for the outbox table under the Sprint 02 prefix convention
- [ ] The domain write and the outbox insert commit **in one transaction**. An L5 test kills the connection between them and asserts neither survives — this is the whole guarantee, and it is the one test that must exist
- [x] No module writes to the outbox through another module's repository; the write goes through `shared-kernel`'s port, and ArchUnit asserts it

**The polling relay**
- [x] Poll → publish → mark-dispatched, with the mark committed only after the broker acknowledges
- [x] **At-least-once, explicitly.** A crash between publish and mark republishes; that is correct and consumers must tolerate it (which `EN-EVENT-2` in Sprint 09 makes them do)
- [ ] Claim/lease so two application instances do not relay the same row — proved with two relays racing one table
- [x] Backlog depth and relay lag exposed as Micrometer meters, joining the `EN-OBS-2` set

**The envelope**
- [x] One envelope schema for every event: event id, type, version, aggregate id, occurred-at, **correlation id** carried from the originating request (`NFR-OBS-03`)
- [x] The envelope is versioned from the first event, not from the first breaking change

**The topic catalogue**
- [x] Topic names, partitioning key, and retention documented per topic in one place that later sprints extend rather than invent alongside
- [x] Partition key chosen so per-aggregate ordering holds — the property Sprint 09's ordering guards and Sprint 18's `EN-EVENT-5` both rely on
- [ ] Kafka topics created via `compose.yaml` (stood up by `EN-DATA-1` in Sprint 01), not auto-created at first publish

**Demonstration, not assertion**
- [ ] The deliverable is a demonstration, in the shape `EN-GATE-1` set in Sprint 01: stop the broker, take catalog writes, restart the broker, show every event arrives. A passing suite with the broker up proves nothing about loss

---

## Integration Risk & Dependencies


**Nothing this sprint meets at a gate — and that is the risk.** The backend ships an enabler with no contract surface, and the frontend ships two stories whose endpoints do not exist. The first time either is tested against the other is `G4`, two sprints of divergence later.

The concrete exposure: the admin console is being built against Prism's generated examples for ten write operations. Whatever assumptions those examples encode — field optionality, error shapes on `E1`/`E3` — go unchallenged until Sprint 09. Worth one deliberate read of the `admin` sections of `openapi.yaml` by both developers this sprint, rather than discovering it at the gate.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
