# `references/cqrs-persistence-rules.md`

# CQRS and Persistence Rules

## Purpose

This file governs commands, queries, PostgreSQL persistence, read models, migrations, and cache interaction.

---

## 1. CQRS Principle

Command and query paths have different responsibilities.

Command side:

```text
protect authoritative business transitions
```

Query side:

```text
answer read questions efficiently
```

Do not impose the same model and persistence style on both sides.

---

## 2. Command Side

A command should represent intent.

The canonical path is:

```text
Command
→ Application
→ Aggregate
→ Repository
→ PostgreSQL
```

If events are produced:

```text
business state
+
outbox record
```

must follow the documented transactional semantics.

---

## 3. Query Side

A query is allowed to use query-oriented persistence.

Depending on the documented read model:

```text
JDBC
MongoDB
Elasticsearch
authoritative SQL
```

may be appropriate.

Do not reconstruct aggregates if no domain behavior is being executed.

---

## 4. JPA and JDBC Roles

Follow the persistence-side decisions specified by ECP.

Do not choose JPA or JDBC merely according to personal preference.

The architecture intentionally distinguishes aggregate-oriented write needs from read-oriented query needs.

---

## 5. PostgreSQL Authority

PostgreSQL is the source of authoritative business state under the current architecture.

Do not introduce authoritative state that exists only in:

```text
Redis
MongoDB
Elasticsearch
```

when those stores are specified as derived.

---

## 6. Database Constraints

When a business rule is delegated to a database constraint, the constraint is part of the feature implementation.

Examples may include:

```text
UNIQUE
CHECK
FOREIGN KEY
version-based concurrency
```

Application validation alone is insufficient when the database specification requires enforcement.

---

## 7. Flyway

Production schema evolution belongs to Flyway.

When a feature changes the physical schema:

```text
add versioned migration
update persistence mapping
update tests
```

Do not rely on ORM auto-DDL to define production schema.

---

## 8. Optimistic Locking

For aggregates protected by optimistic concurrency:

```text
load version
perform mutation
update using expected version
detect conflict
```

The application must have a defined outcome for a conflict.

Do not silently overwrite concurrent business changes.

---

## 9. Reservation Model

Where the architecture uses explicit reservation semantics, preserve them.

Do not replace:

```text
hold
commit
release
```

with direct counter mutation if doing so destroys concurrency guarantees defined by the Domain Model.

---

## 10. Query SQL

Complex read SQL is acceptable when the read requirement is complex.

Valid tools include:

```text
joins
CTEs
aggregation
window functions
keyset pagination
subqueries
```

Do not create unnecessary abstraction layers merely to make SQL visually shorter.

Keep query semantics discoverable.

---

## 11. Pagination

For paginated queries determine explicitly:

```text
offset or cursor
stable ordering
tie-breaking
cursor contents
filter binding
page size
has-more behavior
```

A cursor must represent the ordering semantics sufficiently to continue without duplicates or omissions.

---

## 12. Projection Rules

A projection is derived state.

Therefore it requires:

```text
defined source event
idempotency
ordering behavior
rebuild strategy
consistency classification
```

A projection without a rebuild strategy risks becoming a second source of truth.

---

## 13. Read-After-Write

When a caller immediately reads after a successful command, determine whether the use case requires:

```text
authoritative read
```

or permits:

```text
eventually consistent projection
```

Do not promise immediate projection consistency if the architecture does not provide it.

---

## 14. Redis Cache

For cache-aside use:

```text
read cache
→ miss
→ read authoritative source
→ populate cache
```

On authoritative mutation apply the documented invalidation mechanism.

TTL is not automatically an adequate substitute for invalidation.

---

## 15. Derived Store Failure

A failure in a derived read store must not mutate authoritative data.

Determine whether the system should:

```text
degrade
return unavailable
fall back to an authoritative path
```

according to the relevant use case and architecture.

Do not invent fallback semantics silently.

---

## 16. Persistence Checklist

Before implementing persistence determine:

```text
What is authoritative?

What schema changes?

Which constraints enforce invariants?

Which index supports the access pattern?

Does concurrency matter?

Is this write or read side?

Is a projection involved?

What is the consistency expectation?

How is derived state rebuilt?
```
