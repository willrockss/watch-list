# Feature Specification: Download Content Process Management

**Feature Branch**: `001-download-content-process`

**Created**: 2026-09-20

**Status**: Draft

**Input**: User description: "Coordinate the end-to-end download of a selected content item through qBittorrent: enqueue, start, and track completion of a single download process while respecting capacity limits (disk space and ready-to-watch quota)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enqueue and Start a Selected Download (Priority: P1)

When content is selected for download, the system records a new download process and, as soon as conditions allow, hands it to the downloader paused, then starts it. The user can see the download progress in qBittorrent and the system knows exactly which content item maps to which torrent.

**Why this priority**: This is the core journey - without it nothing ever downloads. Everything else (gates, completion) wraps this flow.

**Independent Test**: Publish a content-selected event with a torrent file path and observe: a persisted process row, the torrent added paused, then started, and the corresponding events published. Delivers the full enqueue-and-start value alone.

**Acceptance Scenarios**:

1. **Given** a content item is selected for download with a torrent file path, **When** the selection event is handled, **Then** a download process is persisted with status INITIAL, the content item identity, and the torrent file path.
2. **Given** an INITIAL download process, qBittorrent available, enough free disk space, and the ready-to-watch quota not reached, **When** the periodic tick runs, **Then** the torrent is added to qBittorrent paused, the torrent hash and content path are captured, the process transitions to PROCESSING, and a started event is published.
3. **Given** several non-finished processes, **When** the tick runs, **Then** only the oldest process is started; all others remain queued, so at most one process is actively processing at a time.

---

### User Story 2 - Respect Capacity Limits (Priority: P2)

The system protects the machine and the viewing pipeline: it will not start a download when the downloader's free disk space falls below a configured minimum, or when the number of movies already ready to watch meets or exceeds a configured maximum. Processes are not lost - they simply wait and are retried on later ticks.

**Why this priority**: Without these gates the disk can fill up and the ready-to-watch backlog can grow unboundedly. It sits on top of the P1 flow, gating its start step.

**Independent Test**: With either gate condition active, run ticks and verify no process is started, no process is dropped, and the gate is logged once. Delivers protection independent of the completion logic.

**Acceptance Scenarios**:

1. **Given** free disk space below the configured minimum, **When** the tick runs with a process waiting to start, **Then** the process is not started, remains queued, is retried on subsequent ticks, and the insufficient-space condition is logged only once per cache refresh.
2. **Given** the ready-to-watch movie count is at least the configured maximum, **When** the tick runs with a process waiting to start, **Then** the process is not started, remains queued, is retried on subsequent ticks, and the quota condition is logged only once per cache refresh.
3. **Given** the free disk space query fails, **When** the tick runs, **Then** the system treats the situation as insufficient disk space (fail-safe) and does not start any process.
4. **Given** qBittorrent is unavailable, **When** the tick runs, **Then** the whole tick is skipped without state changes and the condition is logged.

---

### User Story 3 - Track Download Completion (Priority: P3)

The system polls actively downloading processes and, once a torrent finishes, records the process as finished and notifies interested parties, so the content can be promoted to ready-to-watch.

**Why this priority**: The system already delivers completed files via qBittorrent; this story adds convergence and notification. Least critical because the actual bits arrive regardless, but required for the process lifecycle to close.

**Independent Test**: Prepare a process in PROCESSING, mark its torrent as finished, run a tick, and observe the persisted FINISHED status plus a finished event. Works on its own, given a PROCESSING process exists.

**Acceptance Scenarios**:

1. **Given** a PROCESSING process whose torrent has finished downloading, **When** the tick runs, **Then** the process is persisted as FINISHED and a finished event for the content item is published.
2. **Given** a PROCESSING process whose torrent is still downloading, **When** the tick runs, **Then** the process stays PROCESSING and no event is published.

---

### Edge Cases

- What happens when the torrent cannot be found in qBittorrent when starting? The attempt fails without marking the process failed; the process is retried on later ticks.
- How does the system handle a torrent that disappears mid-download? It logs the condition, keeps the process PROCESSING, and retries on later ticks.
- How does the system recover after a restart? Active processes are reloaded from the database when the cache is refreshed, so no process is lost.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST persist a new download process with status INITIAL, the content item identity, and the torrent file path when a content-selected-for-download event is received.
- **FR-002**: System MUST periodically (fixed tick, default every 60 seconds) drive active, non-finished download processes toward completion.
- **FR-003**: System MUST skip the entire tick when qBittorrent is unavailable.
- **FR-004**: System MUST keep an in-memory cache of active processes, refreshing it when it expires (15-minute TTL) or is invalidated by a state change, and MUST order cached processes oldest-first by creation time.
- **FR-005**: System MUST NOT start a download while free disk space on qBittorrent is below the configured minimum (`download.min-free-disk-space`, default 50 GB); a failed free-space query MUST be treated as insufficient space (fail-safe).
- **FR-006**: System MUST NOT start a download while the count of movies ready to watch is at least the configured maximum (`download.max-ready-to-watch`, default 4).
- **FR-007**: System MUST enqueue an INITIAL process in qBittorrent in paused state, capture the resulting torrent hash and content path, transition the process to PAUSED, persist it, and publish an enqueued event.
- **FR-008**: System MUST start an INITIAL or PAUSED process when capacity gates pass, transition it to PROCESSING, persist it, and publish a download-started event.
- **FR-009**: At most one process MUST be in PROCESSING status at any time; processes are advanced oldest-first.
- **FR-010**: System MUST check a PROCESSING process each tick and, when its torrent is finished, transition it to FINISHED, persist it, and publish a download-finished event.
- **FR-011**: Processes MUST NOT transition to ERROR during this feature; any failed attempt is retried on subsequent ticks indefinitely until processed.
- **FR-012**: Every status transition MUST be persisted to PostgreSQL before the external downloader is advanced to the next action (enqueue → start → verify finish), so a crash never desynchronizes the system and the downloader.
- **FR-013**: After restart, the coordinator MUST recover all active processes from the database when the cache refreshes.
- **FR-014**: Transient gate conditions (disk space, ready-to-watch quota) MUST be logged only once per cache refresh, not on every tick.

### Key Entities *(include if feature involves data)*

- **DownloadContentProcess**: The download aggregate. Attributes: id, status (INITIAL → PAUSED → PROCESSING → FINISHED; ERROR exists as a defined value but is not produced in this scope), content item identity, torrent file path, torrent info hash, content path, creation and update timestamps. `runIteration`, `nextRunAfter`, and `contextInfo` are reserved for future features and are persisted but not used here.
- **ContentItemIdentity**: The external identifier of the content item (e.g., a Kinopoisk id); the stable link between the system and qBittorrent tags.
- **QBitClient**: Port to the downloader exposing: add torrent paused, find torrent by tag, start torrent, availability probe, and free disk space query.
- **Events**: content-selected-for-download (input), content-item-enqueued, content-item-download-started, content-item-download-finished (outputs).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Any non-finished process converges to FINISHED within a bounded number of ticks after all gates pass and its torrent completes (verified by integration test with a simulated qBittorrent).
- **SC-002**: Across consecutive ticks, at most one process is in PROCESSING status at any moment.
- **SC-003**: Each persisted transition (enqueued, started, finished) is accompanied by exactly one matching event publication.
- **SC-004**: Active processes survive a coordinator restart: after restart, all active processes are restored from the database with no loss.
- **SC-005**: No process ever transitions to ERROR; failed attempts are retried indefinitely instead.

## Assumptions

- A single coordinator instance runs (the in-memory cache is authoritative); clustering is out of scope.
- A single qBittorrent instance manages all torrents.
- PostgreSQL persistence with the existing `download_content_process` schema is available.
- The periodic tick is fixed at 60 seconds; the cache TTL is 15 minutes.
- A content-selected-for-download event always carries a torrent file path.
- `runIteration`, `nextRunAfter`, and `contextInfo` are reserved for a future feature and are out of scope for this specification.