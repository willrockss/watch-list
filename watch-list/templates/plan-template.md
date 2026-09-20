# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [`specs/[###-feature-name]/spec.md`](specs/[###-feature-name]/spec.md)

**Input**: Feature specification from `specs/[###-feature-name]/spec.md`

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

**Language/Version**: Java 21, Gradle (Spring Boot 3.3.x)

**Primary Dependencies**: [e.g., Spring Web, spring-boot-starter-jdbc, springdoc-openapi or NEEDS CLARIFICATION]

**Storage**: PostgreSQL

**Testing**: JUnit 5, AssertJ, Testcontainers (PostgreSQL), WireMock; integration tests tagged `IntegrationTest` (Gradle task `integrationTest`), unit tests via `unitTest`

**Target Platform**: Linux server (Docker image, `bootBuildImage`)

**Project Type**: web service / scheduled worker

**Performance Goals**: [domain-specific or N/A]

**Constraints**: [domain-specific, e.g., p95 latency, single-node assumptions, external service limits]

**Scale/Scope**: [domain-specific, e.g., number of users/processes]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [ ] **Article I (Layering)**: `app/` does not import `infra/`; new ports declared where needed
- [ ] **Article II (Integration-first)**: Testcontainers/WireMock used; new behavior covered by `IntegrationTest`
- [ ] **Article III (Test-first)**: Tests written and failing before implementation
- [ ] **Article IV (Explicit persisted transitions)**: State changes explicit and persisted
- [ ] **Article V (Observability)**: Log-once gates; structured logging
- [ ] **Article VI (Simplicity)**: Framework features used directly; no speculative abstraction

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file
├── research.md          # Library/compat research
├── data-model.md        # Entities and DB schema
├── quickstart.md        # Key validation scenarios
├── contracts/           # API/event contracts
└── tasks.md             # Executable task list
```

### Source Code (repository root)

```text
src/main/java/io/kluev/watchlist/
├── app/                 # use cases, aggregates, ports, events
│   └── [feature]/
├── domain/              # shared entities/repositories
├── infra/               # adapters, DAO, clients
│   └── [feature]/
└── common/

src/test/java/io/kluev/watchlist/
├── unit/
└── integration/         # @Tag("IntegrationTest")
```

**Structure Decision**: [Document the selected structure and reference the real directories]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., extra module] | [current need] | [why simpler insufficient] |