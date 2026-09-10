# ecommerce-backend-spring

Gradle multi-project build for the Enterprise Commerce Platform (ECP) backend — a Spring Modulith modular monolith. See [`docs/SA-docs/02-backend/Module Dependency Diagram.md`](../docs/SA-docs/02-backend/Module%20Dependency%20Diagram.md) for the full architecture rationale.

## Layout

Root project `ecp` with 14 subprojects:

- `shared-kernel` — value objects only (`Money`, typed IDs, `Address`); zero outbound dependencies.
- `identity`, `catalog`, `inventory`, `cart`, `ordering`, `payment`, `shipping`, `promotion`, `review`, `notification`, `audit`, `reporting` — one Gradle subproject per bounded context. Each has `api/` (its public surface), `application/`, `domain/`, `infrastructure/` (internal).
- `app` — the only subproject applying the Spring Boot plugin; composition root, produces the `bootJar`.

Cross-module edges are limited to exactly what [`Module Dependency Diagram.md`](../docs/SA-docs/02-backend/Module%20Dependency%20Diagram.md) §3.1–§3.2 allows: `ordering → cart, inventory, promotion`; `cart → catalog, promotion`; every context → `identity`; every module → `shared-kernel`. This is enforced two ways — Gradle project dependencies (undeclared edge fails to compile) and `@ApplicationModule(allowedDependencies = {...})` on each module's `package-info.java` (declared-but-undeclared edge fails `ApplicationModules.verify()`).

**Sprint 0 scope note:** `app` currently depends only on `spring-boot-starter-webmvc`, `spring-boot-starter-actuator`, and `spring-modulith-starter-core`. No persistence/messaging starters (JPA, Kafka, Redis, etc.) are wired yet — those are added to individual modules' `infrastructure` layer as real domain code lands in later sprints.

## Running

```sh
./gradlew build          # compiles all 14 modules
./gradlew check          # runs test (L1-L3) + integrationTest (L4-L6) in every module
./gradlew :app:bootRun   # starts the application on :8080
curl localhost:8080/healthz   # -> OK
```

`./gradlew check` needs Docker for the `integrationTest` (L4-L6) source set — `docker compose up` brings up the local data tier (PostgreSQL, Kafka, Elasticsearch, MongoDB, Redis) from `compose.yaml`, and Testcontainers starts its own PostgreSQL container for `app`'s `PostgresConnectivityIT` independently of Compose.

**On Colima:** start it first (`colima start --memory 4`, per the repo root `CLAUDE.md`), then point Testcontainers at the Colima VM's Docker socket — the default `docker context` resolution isn't enough on its own:

```sh
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock   # the socket path inside the Colima VM, not the host path
```

Testcontainers reuse is a per-developer opt-in, not a repo setting — add `testcontainers.reuse.enable=true` to `~/.testcontainers.properties` to keep a container warm across local `integrationTest` runs. Leave it unset in CI.

## Planted-violation demo

To demonstrate that a structural mistake fails the build (not just review), on the running build:

1. In `catalog/build.gradle.kts`, add `implementation(project(":payment"))` — an edge `catalog`'s `package-info.java` does not declare in `allowedDependencies`. (Don't pick `ordering` for this: `cart -> catalog -> ordering -> cart` is already a real edge set, so routing through `ordering` creates a genuine Gradle-level circular task dependency instead of the intended Modulith-level violation.)
2. Add a class in `payment.api` (e.g. `Marker`) and reference it from a new class in `catalog.api`.
3. Run `./gradlew :app:test --tests ModularityTests` — it fails with a `org.springframework.modulith.core.Violations` report naming the undeclared `catalog -> payment` edge.
4. Delete both added classes and revert `catalog/build.gradle.kts`, then re-run — it passes.

### ArchUnit's four planted violations (`EN-GATE-1`)

`app/src/test/java/org/phuchoang/ecp/ArchitectureTests.java` carries the layer and forbidden-edge rules of [`Module Dependency Diagram.md`](../docs/SA-docs/02-backend/Module%20Dependency%20Diagram.md) §6-§7, on top of the module-boundary check above. Four violations are rehearsed the same way (add → `./gradlew :app:test` fails → revert → passes): `review -> payment`, `inventory -> ordering` (caught by Gradle's own circular-dependency check before Modulith even runs, since `ordering -> inventory` is already a real edge), a `domain` class referencing `identity`, and a class reaching into another module's `application` package. See the Sprint 01 backlog's Review Notes for the exact failure messages each one produces.
