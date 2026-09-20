# Repository Guidelines

## Project Structure & Module Organization

This is the ECP backend: a Java 21 Gradle multi-project Spring Modulith modular monolith. Each bounded context is a top-level module (`identity`, `catalog`, `inventory`, `cart`, `ordering`, `payment`, `shipping`, `promotion`, `review`, `notification`, `audit`, or `reporting`). Keep a module's public contract in `src/main/java/.../<module>/api`; use its `application`, `domain`, and `infrastructure` packages for internals. `shared-kernel` contains dependency-light shared value objects and APIs. `app` is the Spring Boot composition root, web layer, configuration, and Flyway migrations in `app/src/main/resources/db/migration`.

Respect the declared Gradle and `@ApplicationModule` boundaries. Do not depend on another module's internal packages; expose required behavior through that module's `api` package.

You can find the document implementation plan in [Docs](../docs-extract/)

You can find the Sprint plan of this project in [Plan](docs/scrum-plan)

You can find the Skills of this project in [Skills](.skills)

You can find the subagent definition of this project in [Subagents](.agents)

## Build, Test, and Development Commands

- `./gradlew build` compiles and tests every module.
- `./gradlew test` runs unit and architectural tests.
- `./gradlew :app:test --tests ModularityTests` checks Modulith boundaries only.
- `./gradlew check` also runs the `app` Testcontainers integration suite; Docker must be running.
- `docker compose up` starts the local data services; `./gradlew :app:bootRun` starts the API on port 8080.

Run a focused module test with `./gradlew :identity:test`. Initialize the documentation submodule after cloning with `git submodule update --init --recursive`.

## Coding Style & Naming Conventions

Use Java 21, four-space indentation, and the existing idiomatic Spring/Java style. Package names are lowercase under `org.phuchoang.ecp`; classes and records use `PascalCase`, methods and fields use `camelCase`, and constants use `UPPER_SNAKE_CASE`. Name application services by intent — one focused use case per class (for example, `CreateProductService`), or one cohesive capability where the workflows share collaborators (for example, `RegistrationUseCases`) — and tests with a `Test` suffix. Preserve package-level `package-info.java` Modulith declarations when changing dependencies; every nested `api.*` sub-package needs its own `@NamedInterface("api")` package-info, because Spring Modulith 2.x scopes a package-level named interface to that single package.

## Testing Guidelines

Use JUnit 5; unit tests live in `src/test/java` alongside their module. Put Docker/Testcontainers-backed coverage in `app/src/integrationTest/java` and name it `*IT` (for example, `IdentityApiIT`). Add focused regression tests for new behavior and run the narrowest relevant Gradle task before the full `check`.

## Commit & Pull Request Guidelines

Follow the established Conventional Commit style: `feat:`, `feat(backend):`, `build(backend):`, `chore:`, `docs:`, or `security:` followed by a concise imperative summary. Keep commits scoped to one concern. PRs should describe the behavior and module-boundary impact, link the relevant issue or sprint item, list validation performed, and include API examples or screenshots when externally visible behavior changes.

## Configuration & Database Changes

Keep secrets out of `application.yml`. Add immutable Flyway migrations using the existing `VyyyyMMddHHmm__module_description.sql` pattern; do not edit an applied migration.

## Addition Commands

```sh
python3.14 * # the aterisk stands for files or commands that you want to use. Don't use default Python environment of MacOS
```
If you want to use Docker, you need to start the colima engine first.

```sh
colima start --memory 4
```