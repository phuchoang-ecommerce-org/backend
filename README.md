# ecommerce-backend-spring

Gradle multi-project build for the Enterprise Commerce Platform (ECP) backend — a Spring Modulith modular monolith. See [`docs/SA-docs/02-backend/Module Dependency Diagram.md`](docs/SA-docs/02-backend/Module%20Dependency%20Diagram.md) for the full architecture rationale.

> Split out of the original `phuchoang2005/ecommerce` monorepo into its own repo under the `phuchoang-ecommerce-org` organization, with full git history preserved. Architecture/product/PM documentation lives in the sibling [`docs`](https://github.com/phuchoang-ecommerce-org/docs) repo, pulled in here as a git submodule at `docs/` (see that repo's README). Run `git submodule update --init --recursive` after cloning.

## Layout

Root project `ecp` with 14 subprojects. The 13 library modules are grouped
under [`modules/`](modules/); their Gradle project paths remain flat (for
example, `modules/identity` is still `:identity`):

- `shared-kernel` — value objects only (`Money`, typed IDs, `Address`); zero outbound dependencies.
- `identity`, `catalog`, `inventory`, `cart`, `ordering`, `payment`, `shipping`, `promotion`, `review`, `notification`, `audit`, `reporting` — one Gradle subproject per bounded context. Each has `api/` (its public surface), `application/`, `domain/`, `infrastructure/` (internal).
- `app` — the only subproject applying the Spring Boot plugin; composition root, produces the `bootJar`.

Cross-module edges are limited to exactly what [`Module Dependency Diagram.md`](docs/SA-docs/02-backend/Module%20Dependency%20Diagram.md) §3.1–§3.2 allows: `ordering → cart, inventory, promotion`; `cart → catalog, promotion`; every context → `identity`; every module → `shared-kernel`. This is enforced two ways — Gradle project dependencies (undeclared edge fails to compile) and `@ApplicationModule(allowedDependencies = {...})` on each module's `package-info.java` (declared-but-undeclared edge fails `ApplicationModules.verify()`).

**Sprint 0 scope note:** `app` currently depends only on `spring-boot-starter-webmvc`, `spring-boot-starter-actuator`, and `spring-modulith-starter-core`. No persistence/messaging starters (JPA, Kafka, Redis, etc.) are wired yet — those are added to individual modules' `infrastructure` layer as real domain code lands in later sprints.

## Running

Only Docker with Compose is required on the host. Use the containerized Gradle
wrapper for builds and tests; the JDK, Gradle runtime, dependency cache, and
Testcontainers Docker access are provided by the `gradle` Compose service.

```sh
./gradlew-docker build          # compiles and tests all 14 modules
./gradlew-docker check          # runs test (L1-L3) + integrationTest (L4-L6)
./gradlew-docker :identity:test # runs one module's tests
```

The named `gradle-cache` volume keeps wrapper distributions and dependencies
between runs. Source and build outputs stay in the working tree and retain the
host user's ownership. Any Gradle arguments can be passed after
`./gradlew-docker`.

Compose also keeps PostgreSQL and correctness-bearing Redis state in the named
`postgres-data` and `redis-state-data` volumes, so container recreation does
not erase them. `docker compose down -v` intentionally removes those volumes.

The `app` Compose profile runs the API without a local JDK. Kafka keeps
separate internal (`kafka:29092`) and host (`localhost:9092`) listeners.
Running Compose without a profile still starts only the data services:

```sh
docker compose up -d postgres kafka redis-cache redis-state
```

Direct `./gradlew` commands remain available for environments that already
provide JDK 21, but they are not required for local development.

### Runtime profiles

Use exactly one profile. Both profiles enforce bearer authentication and server-side role checks.

- `dev`: generates a temporary JWT keypair if key files are absent and restores five verified
  accounts (one per authenticated role), two categories, four products, and five variants at
  startup. Fixture IDs are stable; existing rows are left as edited. Sign in through
  `POST /api/v1/sessions` with the accounts in [`docs/local-demo.md`](docs/local-demo.md).
- `prod`: requires configured JWT key files and PostgreSQL connection values. It runs schema and
  reference-role migrations but creates no mock accounts or catalog fixtures. Flyway clean remains
  disabled and Hibernate only validates the schema.

#### Start the dev profile

The default app command selects Spring's `dev` profile and loads
[`docker/app/dev.env`](docker/app/dev.env), whose connection values use Compose
service names:

```sh
./compose-docker --profile app up --build app
curl localhost:8080/healthz   # -> OK
```

Stop it with `./compose-docker --profile app down`.

Or start the dev profile with  

```sh
./gradlew :app:bootRun --args='--spring.profiles.active=dev'
```

#### Start the prod profile locally

Copy the production template and replace its database password, cursor signing
key, JWT key paths, and any integration secrets. `.env.prod` and `.secrets/`
are ignored by Git. The JWT files must be an RSA PKCS#8 private key and matching
X.509 public key at paths visible inside the container.

```sh
cp .env.prod.example .env.prod
mkdir -p .secrets
# Add .secrets/jwt-private.pem and .secrets/jwt-public.pem, then edit .env.prod.

APP_PROFILE=prod APP_ENV_FILE=.env.prod \
  ./compose-docker --env-file .env.prod --profile app up --build app
curl localhost:8080/actuator/health/readiness
```

or start the prod profile with:

```sh
./gradlew :app:bootRun --args='--spring.profiles.active=prod'
```

This command uses Spring's `prod` profile against the local Compose data tier,
but does not seed demo accounts or catalog data. For external infrastructure,
replace the PostgreSQL, Kafka, and Redis endpoints in `.env.prod`. Keep the
`APP_PROFILE`, `APP_ENV_FILE`, and `--env-file .env.prod` settings on every
Compose command, including `down`, so Compose resolves the same app
configuration.

When `bootRun` is invoked directly rather than through the `app` Compose
service, it still loads `.env` when present and falls back to `.env.example`.

For a real local frontend/backend walkthrough with seeded session and catalog records, see
[`docs/local-demo.md`](docs/local-demo.md).

`./gradlew-docker check` passes the Docker socket into the build container for the `integrationTest` (L4-L6) source set. Testcontainers starts its own PostgreSQL container for `app`'s `PostgresConnectivityIT` independently of Compose; the local data tier does not need to be running first. The build container maps `host.docker.internal` to Docker's host gateway so the Testcontainers cleanup process can call back into the test JVM.

On Linux, `gradlew-docker` detects the Docker socket's group automatically. If
the daemon uses a nonstandard socket or remote transport, set `DOCKER_GID` and
adapt the socket volume in `compose.yaml` for that daemon.

Testcontainers reuse is a per-developer opt-in, not a repo setting — add `testcontainers.reuse.enable=true` to `~/.testcontainers.properties` to keep a container warm across local `integrationTest` runs. Leave it unset in CI.

## Planted-violation demo

To demonstrate that a structural mistake fails the build (not just review), on the running build:

1. In `modules/catalog/build.gradle.kts`, add `implementation(project(":payment"))` — an edge `catalog`'s `package-info.java` does not declare in `allowedDependencies`. (Don't pick `ordering` for this: `cart -> catalog -> ordering -> cart` is already a real edge set, so routing through `ordering` creates a genuine Gradle-level circular task dependency instead of the intended Modulith-level violation.)
2. Add a class in `payment.api` (e.g. `Marker`) and reference it from a new class in `catalog.api`.
3. Run `./gradlew :app:test --tests ModularityTests` — it fails with a `org.springframework.modulith.core.Violations` report naming the undeclared `catalog -> payment` edge.
4. Delete both added classes and revert `modules/catalog/build.gradle.kts`, then re-run — it passes.

### ArchUnit's four planted violations (`EN-GATE-1`)

`app/src/test/java/org/phuchoang/ecp/ArchitectureTests.java` carries the layer and forbidden-edge rules of [`Module Dependency Diagram.md`](docs/SA-docs/02-backend/Module%20Dependency%20Diagram.md) §6-§7, on top of the module-boundary check above. Four violations are rehearsed the same way (add → `./gradlew :app:test` fails → revert → passes): `review -> payment`, `inventory -> ordering` (caught by Gradle's own circular-dependency check before Modulith even runs, since `ordering -> inventory` is already a real edge), a `domain` class referencing `identity`, and a class reaching into another module's `application` package. See the Sprint 01 backlog's Review Notes for the exact failure messages each one produces.
