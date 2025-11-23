# Work in Progress Summary

## Purpose
This file summarizes the current work being performed on the notification system: which fixes are being applied, which files were edited, current blockers, and the next steps. This is a living single-file summary intended for the team lead or reviewer.

---

## High-level goal
Apply all fixes from the `COMPREHENSIVE_PROBLEMS_MATRIX.md` and `IMPLEMENTATION_STRATEGIES.md` into the codebase so the notification pipeline is robust, idempotent, and observable without creating conflicting changes or historical noise.

## What I'm doing now
- Implementing distributed locking and orchestration fixes to avoid race conditions and event-loss.
- Applying the "operation order" fix so ProcessedEvent is inserted before the RabbitMQ publish.
- Preparing DLQ (dead-letter) support and retry/backoff for both producer and consumer paths.
- Adding monitoring hooks (DLQ monitor, delivery latency) and cleanup/archive jobs.

## Files already created/modified (current session)
- `notifications_system/COMPREHENSIVE_PROBLEMS_MATRIX.md`  — complete problems matrix
- `notifications_system/IMPLEMENTATION_STRATEGIES.md`     — concrete implementations with code snippets
- `greedys_api/.../EventOutboxOrchestrator.java`         — small import update (further edits planned)

## Immediate next changes (in progress)
1. Create `DistributedLockService` (DB-backed) and wire it into `EventOutboxOrchestrator`.
2. Finish updating `EventOutboxOrchestrator` to:
   - Insert `ProcessedEvent` first (idempotency lock)
   - Publish to RabbitMQ
   - Update EventOutbox status
   - Track attemptCount and move to `FAILED/DEAD_LETTER` after max attempts
3. Add DLQ support in RabbitMQ config and update listeners to NACK → DLQ after N retries.
4. Implement scheduled cleanup/archive jobs for EventOutbox and CustomerNotification.
5. Add `DeliveryLatencyMonitor` and `DLQMonitoringService` for alerts and metrics.

## Blockers / Notes
- Some helper services (e.g. `AlertService`, `DistributedLockService`) do not exist yet; creating them next.
- Existing run of `mvn clean compile` reported no fatal failures in top-level build, but compilation warnings exist due to missing imports introduced earlier; will address by creating the missing classes.
- We'll run incremental compiles after each core change and fix any compile errors found.

## Safety & compatibility
- Changes will be additive and low-risk where possible: introduce new columns (retry_count exists), DLQ queues, and scheduled jobs.
- For race conditions we prefer DB-backed locks to avoid adding external dependencies immediately.
- Tests will be added for crash and retry scenarios (happy path + one failure case).

## Very short timeline (estimate)
- Distributed lock + orchestrator update: 1-2 hours
- DLQ + listener updates: 1-2 hours
- Cleanup/archive jobs + monitoring: 1 hour
- Tests + compile fixes: 1-2 hours

---

If you want, I can now: (A) continue implementing `DistributedLockService` and finish `EventOutboxOrchestrator` changes, (B) implement DLQ config and listener changes next, or (C) stop and wait for your approval to proceed. Please choose A/B/C.