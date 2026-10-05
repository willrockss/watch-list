# Tasks: Download Content Process Management

**Input**: Design documents from `specs/001-download-content-process/`

**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Integration tests are required by the feature specification (User Stories' acceptance scenarios and success criteria SC-001/SC-003) and the Watch-List Constitution (Articles II & III — test-first, integration-first). Run unit tests: `./gradlew unitTest`; integration tests: `./gradlew integrationTest`.

**Organization**: Tasks are grouped by user story. The persistence layer (`download_content_process` schema + `DownloadContentProcessDao`) and the qBittorrent adapter already exist; the production delta is confined to `DownloadProcessCoordinator`. All new tests live in one integration class: `src/test/java/io/kluev/watchlist/app/downloadcontent/DownloadProcessCoordinatorIntegrationTest.java`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Production**: `src/main/java/io/kluev/watchlist/{app,domain,infra}/...`
- **Integration tests**: `src/test/java/io/kluev/watchlist/...` with `@Tag("IntegrationTest")`
- Run unit tests: `./gradlew unitTest`; integration tests: `./gradlew integrationTest`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Baseline verification — project scaffolding, build, and test tasks already exist.

- [x] T001 Verify baseline build: `./gradlew compileJava` succeeds on `main`.
- [x] T002 Verify baseline tests pass: `./gradlew unitTest` succeeds (no new scaffolding needed).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persistence layer MUST be confirmed working before any coordinator behavior is verified.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Confirm `download_content_process` schema and DAO status round-trip: run existing `src/test/java/io/kluev/watchlist/infra/downloadcontent/DownloadContentProcessDaoIntegrationTest.java` via `./gradlew integrationTest --tests "io.kluev.watchlist.infra.downloadcontent.DownloadContentProcessDaoIntegrationTest"` — must be green.

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Enqueue and Start a Selected Download (Priority: P1) 🎯 MVP

**Goal**: On `ContentSelectedForDownload`, persist a process as `INITIAL`; on each tick, enqueue it into qBittorrent paused and persist `PAUSED` (publishing `content-item-enqueued`) regardless of capacity; then start it (`PROCESSING`, publishing `content-item-download-started`) as soon as gates pass.

**Independent Test**: Publish `ContentSelectedForDownload`, run `tick()`, and observe: persisted `INITIAL` row, `QBitClient.addTorrPaused` called once, DB status advances to `PAUSED` (then `PROCESSING` when gates pass), and exactly one `ContentItemEnqueuedEvent` + `ContentItemDownloadStartedEvent`.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T010 [US1] Create `src/test/java/io/kluev/watchlist/app/downloadcontent/DownloadProcessCoordinatorIntegrationTest.java` with Testcontainers PostgreSQL, `@MockBean QBitClient`, `@MockBean MovieRepository`, `@MockBean GoogleSheetsClient`, an event-recording listener (`@TestConfiguration`), and `spring.temporal.start-workers=false`. Test: publishing `ContentSelectedForDownload` persists a process with status `INITIAL`, identity, and torrent file path (US1 AS1, FR-001).
- [x] T011 [US1] Test: given an `INITIAL` process, qBittorrent available, enough disk space, and quota not reached, one `tick()` enqueues paused (torrent hash + content path captured), persists `PAUSED` → `PROCESSING`, and publishes enqueued + started events exactly once each (US1 AS2+AS3, FR-007/008/012, SC-003).

### Implementation for User Story 1

- [x] T012 [US1] Edit `src/main/java/io/kluev/watchlist/app/downloadcontent/DownloadProcessCoordinator.java`: make the enqueue step unconditional — remove the `if (!hasEnoughReadyToWatchMovies()) { ... }` guard around `enqueueNewAsPaused()` so every tick enqueues all `INITIAL` processes when qBittorrent is available and the cache is non-empty (FR-005/006/007).
- [x] T013 [US1] Keep the `INITIAL` switch branch as a defensive fallback that enqueues+starts directly when gates pass (FR-008); no structural refactor of the tick flow.
- [x] T014 [US1] Add/adjust logging for the enqueue step to clearly distinguish "enqueued paused" from "started" (observability per Article V).

**Checkpoint**: User Story 1 fully functional and testable independently.

---

## Phase 4: User Story 2 - Respect Capacity Limits (Priority: P2)

**Goal**: Capacity gates (disk space, ready-to-watch quota) block ONLY the start step. Every `INITIAL` process is still enqueued paused and persisted as `PAUSED` — visible in qBittorrent and the status table — and retried on later ticks; each gate is logged once per cache refresh.

**Independent Test**: With either gate active, run ticks and verify no process is started, every `INITIAL` process is enqueued paused / persisted `PAUSED`, no process is dropped, and the gate is logged once.

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T020 [US2] Test: ready-to-watch quota reached (`MovieRepository.getMoviesReadyToWatch` returns ≥ `download.max-ready-to-watch`) with an `INITIAL` process → `tick()` still calls `addTorrPaused`, persists `PAUSED`, publishes enqueued event, does NOT call `QBitClient.start`, publishes no started event (US2 AS2, FR-006/007). **This is the core regression test for the spec change — fails on current `main`.**
- [x] T021 [US2] Test: free disk space below `download.min-free-disk-space` with an `INITIAL` process → `tick()` enqueues paused / persists `PAUSED`, does not start (US2 AS1, FR-005/007).
- [x] T022 [US2] Test: `PAUSED` process + a gate failing → second `tick()` keeps it `PAUSED`, does not start, does not re-enqueue (no duplicate `addTorrPaused`), and does not re-log the gate condition within the same cache refresh (US2 AS1/AS2 retry semantics, FR-014).
- [x] T023 [US2] Test: `getFreeSpaceOnDisk()` throws → fail-safe: no process started, enqueue still proceeds (US2 AS3, FR-005).
- [x] T024 [US2] Test: `isAvailable()` false → entire tick skipped, zero qBittorrent calls, no state changes (US2 AS4, FR-003).

### Implementation for User Story 2

- [x] T025 [US2] Edit `src/main/java/io/kluev/watchlist/app/downloadcontent/DownloadProcessCoordinator.java`: update the PAUSED-gate log messages from "Keep it initial and retry later" to PAUSED wording (e.g., "Keep it paused and retry later") — the processes are now enqueued paused, not initial (FR-014, Article V).
- [x] T026 [US2] Re-run the failing US2 tests from Phase 4 — they must pass once T012 and T025 land (Green phase of Article III).

**Checkpoint**: User Stories 1 AND 2 both work independently.

---

## Phase 5: User Story 3 - Track Download Completion (Priority: P3)

**Goal**: A `PROCESSING` process whose torrent finished is persisted `FINISHED` and publishes `content-item-download-finished`; otherwise it stays `PROCESSING`. No production change expected — the completion path exists; this phase closes the coverage gap.

**Independent Test**: Prepare a `PROCESSING` process, mark its torrent finished, run a tick, observe persisted `FINISHED` plus a finished event.

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T030 [US3] Test: `PROCESSING` process + `QBitClient.findByIdTagOrNull` returns a finished torrent → `tick()` persists `FINISHED` and publishes `ContentItemDownloadFinishedEvent` exactly once (US3 AS1, FR-010).
- [x] T031 [US3] Test: `PROCESSING` process + torrent still downloading → stays `PROCESSING`, no event (US3 AS2, FR-010).

### Implementation for User Story 3

- [x] T032 [US3] No production code change expected; if a test exposes a defect, fix within `DownloadContentProcess.checkFinished` / `DownloadProcessCoordinator` PROCESSING branch only.

**Checkpoint**: All user stories independently functional.

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Full verification, documentation, and commits.

- [x] T040 Run full verification: `./gradlew unitTest integrationTest` — all green.
- [x] T041 Commit logically: (1) failing tests (Red), (2) production change in `DownloadProcessCoordinator` (Green), (3) docs (`spec.md` revision, `plan.md`, `tasks.md`).
- [x] T042 [P] Review spec `Status: Draft` and mark feature status on the branch after CI green (maintainer decision).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion; proceed in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Aggregate/DAO behavior before coordinator changes; adapters unchanged
- Story complete before moving to next priority

### Parallel Opportunities

- Tasks marked [P] can run in parallel (different files, no dependencies). All other tasks touch the same two files (`DownloadProcessCoordinator.java` and `DownloadProcessCoordinatorIntegrationTest.java`), so they are sequential.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (Article III Red phase)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence