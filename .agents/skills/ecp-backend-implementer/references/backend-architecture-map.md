
# `references/backend-architecture-map.md`

# Backend Architecture Map

## Purpose

This reference provides a compact mental model of ECP's backend architecture for feature implementation.

---

## 1. Backend Shape

Conceptually:

```text
External Caller
      │
      ▼
   ecp-api
Spring Modular Monolith
      │
      ├── Domain Modules
      │
      ├── PostgreSQL
      │
      ├── Redis
      │
      ├── Kafka
      │
      ├── MongoDB projections
      │
      ├── Elasticsearch projection
      │
      └── External Providers
```

The backend remains one deployment unit while preserving explicit module boundaries.

---

## 2. Module-Level Shape

A business module conceptually contains four responsibilities:

```text
API
Application
Domain
Infrastructure / Persistence
```

Think in dependency direction rather than package naming alone.

---

## 3. Command Path

Canonical authoritative mutation:

```text
REST / Module Entry
        ↓
Application Command
        ↓
Application Use Case
        ↓
Aggregate
        ↓
Invariant Enforcement
        ↓
Repository
        ↓
PostgreSQL
        +
Outbox
        ↓
Commit
```

This path changes business truth.

---

## 4. Query Path

Canonical read:

```text
REST Query
     ↓
Query Service
     ↓
JDBC / approved read model
     ↓
Projection / DTO
     ↓
Response
```

Query models are optimized for answering questions rather than enforcing aggregate state transitions.

---

## 5. Event Path

Canonical asynchronous integration:

```text
Business Transaction
      ↓
Outbox Record
      ↓
Commit
      ↓
Outbox Relay
      ↓
Kafka
      ↓
Consumer
      ↓
Projection / downstream module
```

Outbox and business state belong to the same local transaction when event publication is part of the operation.

---

## 6. Source-of-Truth Model

Conceptually:

```text
PostgreSQL
    authoritative business data

Kafka
    durable event backbone

MongoDB
    scoped derived projection

Elasticsearch
    search projection

Redis
    cache / rate limit / specified coordination
```

Never place unrecoverable business authority in a derived store.

---

## 7. Module Interaction

A business relation does not automatically imply a Java dependency.

Possible interaction forms include:

```text
direct permitted module API
published interface
shared-kernel type
domain/integration event
Kafka event
```

Use the Module Dependency Diagram to determine legality.

---

## 8. Feature Location Model

When implementing a feature, locate it along these dimensions:

```text
Business capability
        ↓
Bounded context
        ↓
Module
        ↓
Aggregate or query model
        ↓
Application operation
        ↓
Persistence / integration
        ↓
API
```

This is more useful than starting from class names.

---

## 9. Write vs Read Question

Ask first:

```text
Does this operation change authoritative business state?
```

If yes:

```text
command side
```

If no:

```text
query side
```

Do not confuse:

```text
POST
```

with:

```text
command
```

or:

```text
GET
```

with:

```text
simple aggregate repository lookup
```

CQRS classification follows semantic responsibility.

---

## 10. Consistency Question

For every data dependency determine:

```text
strong/local transactional consistency
eventual consistency
advisory consistency
derived projection
```

Do not introduce synchronous coupling merely to hide eventual consistency when the architecture intentionally accepts it.

---

## 11. Transaction Boundary

The transaction boundary should coincide with the consistency requirement of the owning use case.

A typical local mutation may contain:

```text
aggregate state mutation
database persistence
outbox persistence
```

but not synchronous completion of every downstream module.

Cross-module distributed atomicity must not be invented implicitly.

---

## 12. Feature Impact Surface

A backend feature can propagate through:

```text
Domain
Application
Database
Outbox
Kafka
Projection
Redis
API
Security
Observability
Tests
```

Trace the actual affected surface rather than modifying every layer mechanically.
