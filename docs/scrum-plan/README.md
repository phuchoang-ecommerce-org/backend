<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Delivery Plan — Enterprise Commerce Platform (ECP)

**Document type:** Derived lane plan
**Audience:** Backend Engineering, Product Management, Quality Assurance
**Canonical source:** [Integrated PM plan](../README.md)

---

This is the self-contained backend delivery view of the Enterprise Commerce Platform plan. It contains the work owned by this lane, its delivery sequence, its quality gates, and a complete sprint timeline. The integrated PM plan remains authoritative for shared scope, cross-lane detail, and disputes.

## Documents

| Document | Purpose |
|---|---|
| [Product backlog](./product-backlog.md) | Non-zero BE story and enabler slices, with other-lane delivery milestones |
| [Release plan](./release-plan.md) | All releases, sprint positions, gates, and backend sequencing |
| [Definition of Ready and Done](./definition-of-done.md) | Backend-owned quality gates plus shared story/release criteria |
| [Sprint backlogs](./sprint-backlogs/README.md) | Filtered worklists for every delivery and hardening sprint |

## Shared Authoritative Material

- [Integrated PM plan](../README.md) — canonical work, estimates, and cross-lane schedule.
- [Scrum framework](../scrum-framework.md) — shared cadence and board rules.
- [Integration plan](../integration-plan.md) — normative contract, Contract Sync, and amendment protocol.
- [Business analysis](../../BA-docs/README.md) and [solution architecture](../../SA-docs/README.md) — requirements and implementation constraints.

## Maintenance

Update the canonical PM documents first, then regenerate these views with:

`node PM-docs/scripts/generate-lane-plans.mjs`

Never edit a generated lane document directly.
