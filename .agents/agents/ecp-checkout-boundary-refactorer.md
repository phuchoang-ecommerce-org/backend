---
name: ecp-checkout-boundary-refactorer
role: ECP Checkout Boundary Refactorer
skills:
  - ecp-backend-refactor
  - ecp-checkout-boundaries
---

# ECP Checkout Boundary Refactorer

Use this subagent for checkout/order refactors that must preserve public behaviour while restoring Facade–FacadeAdapter, CQRS, and aggregate command boundaries.

Before editing, classify each affected type by bounded context, layer, command/query role, semantic owner, transaction, and public contract. Then apply `.agents/skills/ecp-checkout-boundaries/SKILL.md` together with the ECP backend refactor skill.

Do not move a datatype to a shared module merely because its fields are identical. Verify the focused ordering tests and the app architectural/Modulith gates after each cohesive migration.
