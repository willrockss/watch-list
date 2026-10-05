# Implementation Plan: Download Content Process Management

**Branch**: `001-download-content-process` | **Date**: 2026-09-20 | **Spec**: [`specs/001-download-content-process/spec.md`](spec.md)

**Input**: Feature specification from `specs/001-download-content-process/spec.md`

## Summary

Coordinate the end-to-end download of a selected content item through qBittorrent: on a `ContentSelectedForDownload` event, persist a download process as `INITIAL`; on each tick unconditionally enqueue it into qBittorrent paused (persist `PAUSED`, publish `content-item-enqueued`); start it only when capacity gates pass (free disk space ≥ `download.min-free-disk-space`, ready-to-watch count < `download.max-ready-to-watch`) by transitioning to `PROCESSING` and publishing `content-item-download-started`; poll until the torrent finishes, then persist `FINISHED` and publish `content-item-download-finished`.

**Behavior change this plan implements** (from spec revision): enqueue must NOT be gated by capacity. Today `DownloadProcessCoordinator.tick()` calls `enqueueNewAsPaused()` only inside `if (!hasEnoughReadyToWatchMovies())`, so when the ready-to-watch quota is reached (and effectively whenever a gate blocks the start) a process lingers in `INITIAL` — invisible in the status table (Google Sheet) and absent from qBittorrent. The change makes the enqueue step unconditional (a paused torrent consumes no disk space and reserves no processing slot) and moves all capacity checks to the start step only, per FR-005/FR-006/FR-007/FR-008.

**Technical approach**: modify `DownloadProcessCoordinator` (the only production class touched):
- remove the `!hasEnoughReadyToWatchMovies()` guard so `enqueueNewAsPaused()` runs every tick when qBittorrent is available and the cache is non-empty;
- align the gate log messages that currently say "Keep it initial" with the PAUSED semantics;
- keep the `INITIAL` switch branch as a defensive fallback (FR-008 permits starting an INITIAL process directly).

The behavior is proven by a new Testcontainers-based integration test (`DownloadProcessCoordinatorIntegrationTest`) that drives the real coordinator, DAO, and PostgreSQL while mocking the `QBitClient` port and `MovieRepository`. Tests are written first and confirmed failing before the implementation change (constitution Article III).

## Technical Context

**Language/Version**: Java 21, Gradle (Spring Boot 3.3.x)

**Primary Dependencies**: spring-boot-starter-web, spring-boot-starter-jdbc, spring-boot-starter-validation, Lombok. Test: spring-boot-starter-test, Testcontainers PostgreSQL 1.20.x, wiremock-spring-boot.

**Storage**: PostgreSQL — `download_content_process` schema already exists; `DownloadContentProcessDao` (`src/main/java/io/kluev/watchlist/infra/downloadcontent/DownloadContentProcessDao.java`) already persists all statuses.

**Testing**: JUnit 5, AssertJ, Testcontainers (PostgreSQL); integration tests tagged `IntegrationTest` (Gradle task `integrationTest`), unit tests via `unitTest`.

**Target Platform**: Linux server (Docker image, `bootBuildImage`)

**Project Type**: scheduled worker / web service

**Performance Goals**: N/A — single low-volume coordinator on a 60-second tick.

**Constraints**: single coordinator instance (in-memory cache is authoritative); single qBittorrent; PostgreSQL persistence; at most one process in `PROCESSING` at any time.

**Scale/Scope**: small number of concurrent downloads (one active at a time); reserved fields (`runIteration`, `nextRunAfter`, `contextInfo`) untouched.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Article I (Layering)**: change confined to `app/downloadcontent/DownloadProcessCoordinator`; no new ports; no `infra/` imports added
- [x] **Article II (Integration-first)**: new behavior covered by `DownloadProcessCoordinatorIntegrationTest` (Testcontainers PostgreSQL + mocked `QBitClient`/`MovieRepository`)
- [x] **Article III (Test-first)**: tests written and confirmed FAILING before the coordinator change
- [x] **Article IV (Explicit persisted transitions)**: `INITIAL → PAUSED → PROCESSING → FINISHED` transitions remain explicit and persisted via `DownloadContentProcessDao.save`; no in-memory-only state
- [x] **Article V (Observability)**: gate conditions still logged once per cache refresh (FR-014); log text updated to PAUSED semantics
- [x] **Article VI (Simplicity)**: no new abstractions, no schema/API changes; minimal diff to the existing coordinator

## Project Structure

### Documentation (this feature)

```text
specs/001-download-content-process/
├── spec.md       # Feature specification (behavior authority)
├── plan.md       # This file
└── tasks.md      # Executable task list
```

`research.md`, `data-model.md`, `quickstart.md`, and `contracts/` are intentionally not created: the change adds no entities, no DB schema changes, and no new external contracts.

### Source Code (repository root)

```text
src/main/java/io/kluev/watchlist/
├── app/
│   ├── downloadcontent/
│   │   ├── DownloadProcessCoordinator.java   # CHANGE: unconditional enqueue + gate log wording
│   │   ├── DownloadContentProcess.java       # unchanged (aggregate transitions)
│   │   ├── DownloadContentProcessStatus.java # unchanged (INITIAL/PAUSED/PROCESSING/FINISHED/ERROR)
│   │   ├── QBitClient.java                   # unchanged (port)
│   │   └── event/*.java                      # unchanged (input/output events)
│   └── event/ContentSelectedForDownload.java # unchanged (input event)

src/test/java/io/kluev/watchlist/
└── app/downloadcontent/
    └── DownloadProcessCoordinatorIntegrationTest.java   # NEW: tick-flow behavior
```

**Structure Decision**: reuse the existing `app/` (aggregate + coordinator + port) and `infra/` (DAO, `QBitClientImpl`) layout exactly as-is. All production changes live in `DownloadProcessCoordinator`; all new verification lives in one integration test class sharing the same Spring context setup pattern already used by `DownloadContentProcessDaoIntegrationTest`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *none*    | —          | —                                    |