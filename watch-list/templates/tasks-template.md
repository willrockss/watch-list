# Tasks: [FEATURE NAME]

**Input**: Design documents from `specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: The examples below include test tasks. Tests are OPTIONAL - only include them if explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

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

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Configure dependencies in build.gradle
- [ ] T003 [P] Configure linting/formatting

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Setup database schema and migrations
- [ ] T005 [P] Create base entities that all stories depend on
- [ ] T006 [P] Configure error handling and logging infrastructure
- [ ] T007 Setup environment configuration management

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Integration test for [user journey] in src/test/java/io/kluev/watchlist/... (Testcontainers/WireMock)

### Implementation for User Story 1

- [ ] T011 [P] [US1] Create [Entity1] in src/main/java/io/kluev/watchlist/app/...
- [ ] T012 [US1] Implement [Service/Coordinator] in src/main/java/io/kluev/watchlist/app/...
- [ ] T013 [US1] Implement [adapter] in src/main/java/io/kluev/watchlist/infra/...
- [ ] T014 [US1] Wire bean in MainBeansConfig
- [ ] T015 [US1] Add validation and error handling
- [ ] T016 [US1] Add logging

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T020 [P] [US2] Integration test for [user journey] in src/test/java/io/kluev/watchlist/...

### Implementation for User Story 2

- [ ] T021 [P] [US2] Implement [component] in src/main/java/io/kluev/watchlist/app/...
- [ ] T022 [US2] Integrate with User Story 1 components (if needed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T030 [P] [US3] Integration test for [user journey] in src/test/java/io/kluev/watchlist/...

### Implementation for User Story 3

- [ ] T031 [P] [US3] Implement [component] in src/main/java/io/kluev/watchlist/app/...
- [ ] T032 [US3] Integrate with User Story 1/2 components

**Checkpoint**: All user stories should now be independently functional

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T040 [P] Documentation updates
- [ ] T041 Code cleanup and refactoring
- [ ] T042 [P] Additional unit tests
- [ ] T043 Security hardening
- [ ] T044 Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion; proceed in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models before services; services before adapters/endpoints
- Story complete before moving to next priority

### Parallel Opportunities

- All tasks marked [P] can run in parallel (different files, no dependencies)
- Once Foundational phase completes, user stories can start sequentially in priority order

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence