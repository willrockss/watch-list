# Watch-List Constitution

## Core Principles

### I. Layered Domain Architecture
The codebase is organized into three layers that MUST NOT leak into each other:

- `app/` — use cases, aggregates, and application events. Contains business rules and ports (interfaces).
- `infra/` — adapters: DAO, HTTP clients, external services (qBittorrent, Telegram, Google Sheets, Kinopoisk, Restate, Temporal). Implements `app/` ports.
- `domain/` — shared domain entities and repositories.

Dependencies point inward: `app/` never imports `infra/`; `infra/` depends on `app/` interfaces. Controllers/endpoints and configuration wire the layers.

### II. Integration-First Testing
Tests MUST verify behavior in realistic environments before relying on unit tests:

- Prefer Testcontainers Postgres over mocks for persistence.
- Use WireMock for external HTTP services.
- Integration tests carry the `@Tag("IntegrationTest")` and run via the `integrationTest` Gradle task; unit tests via `unitTest`.
- Contract behavior (state transitions, published events) MUST be covered by integration tests.

### III. Test-First Imperative
NON-NEGOTIABLE: no implementation code before tests. For every change:

1. Write tests that define the expected behavior (Red phase).
2. Confirm tests FAIL.
3. Implement until tests pass (Green).
4. Refactor while keeping them green.

### IV. Explicit Persisted State Transitions
State machines (e.g., download process lifecycle) MUST transition explicitly and every transition MUST be persisted. No silent state drops, no transient in-memory-only states that matter for correctness.

### V. Observability
- Structured logging with logstash-logback-encoder; logs are JSON in production.
- Log-and-continue for transient conditions (qBittorrent down, disk pressure); log each such gate ONCE per cache refresh, not on every tick.
- Prometheus metrics via Micrometer where operational signals matter.

### VI. Simplicity (Anti-Abstraction / YAGNI)
- Use Spring idioms and framework features directly; no speculative abstraction layers.
- No "might need" or future-proofing code. Unused reserved fields are left as-is until a feature needs them.
- If complexity is required, document the rationale in the plan's Complexity Tracking section.

## Governance

- This constitution supersedes all other practices. Amendments require: documented rationale, maintainer review, and a backwards-compatibility note.
- Every feature spec and plan MUST pass a constitution check before implementation.
- **Version**: 1.0.0 | **Ratified**: 2026-09-20 | **Last Amended**: 2026-09-20