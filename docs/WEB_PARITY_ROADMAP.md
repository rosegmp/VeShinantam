# VeShinantam web parity roadmap

Status: proposed implementation plan

Target: behavioral and data parity between Android and the Compose Multiplatform web app

Planning assumption: one engineer working in focused increments; estimates are indicative, not commitments

Implementation status (2026-09-16): milestone 0 is complete; milestones 1 and 2 are in progress; the milestone-3 schedule creation and management workflows and the milestone-4 Today and Calendar workflows are complete. The canonical shared contract, lossless Android import/export adapter, validation/round-trip tests, version-2 web backup envelope, IndexedDB migration, durable Android and web mutation outboxes, backward-compatible entity-sync clients, deployed entity-sync schema, automatic Android retry worker, shared deterministic planning engine, full 12-program reference catalog, and shared structured material-range catalog are implemented. Android and web now use the same bilingual preset and custom units, verified positions/dates, exact ranges, synchronized exclusions, cadence, pace, finish-by distribution, missed-work behavior, finite/annual chazarah, weekend, and official Oraysa rules. Web Today matches Android's five task sections, completed-today handling, overdue due-date display, collapsible sections, synchronized natural reference ordering, and browser-local day/time-zone re-evaluation without reloading. Web Calendar matches Gregorian and Hebrew month navigation, synchronized primary-calendar choice, filter-aware daily completion states/counts, bilingual cell labels and selected-date context, unrestricted navigation, and individual completion/undo. Web schedule management includes pause/resume, archive/restore, confirmed deletion, separately confirmed bulk completion of past learning and chazarah, and future-only regeneration by pace or finish date while preserving history and completed tasks. Progress and goals now use one shared Android/web calculator for due-through-today completion, learning-unit quantities, reviews, current and longest streaks, milestones, synchronized goals, and the 30-day chazarah workload. Web settings now preserve, validate, back up, and synchronize the full canonical preference record; platform-specific reminder and preset-update behavior is labeled without resetting Android choices. Android and web now export the same versioned canonical backup dataset, validate before confirmed replacement, and retain import compatibility with legacy Android and web files. Browser print/PDF now matches Android's 7/14/30/60/90-day active-task content, localization, reference-language settings, checkboxes, RTL, and A4 pagination. The installable web shell now performs atomic cache upgrades, pre-caches both generated Wasm runtimes, preserves the prior working cache after interrupted installs, uses safe offline navigation/asset fallbacks, exposes a Today shortcut, and refreshes the supported app badge from Android-equivalent due-count rules. Entity sync remains rollout-gated until the updated Android APK is distributed; browser reminders and signed preset updates are next.

Update (2026-09-17): best-effort browser reminders now request permission only from an explicit settings save, schedule the synchronized local reminder time while the web app remains open, use the service worker for localized due-count notifications, and return notification clicks to Today. Reliable closed-app delivery remains a documented Web Push dependency. Signed preset updates are the next milestone-6 gap.

## Outcome

A signed-in user can move between Android and the web without losing data or encountering different scheduling results. Core learning workflows, history, preferences, goals, backup, and printable schedules behave consistently on both platforms. Android-only operating-system integrations receive the closest practical web equivalent and are not allowed to block core parity.

Parity means the same behavior and data, not identical pixels. Each client should remain useful offline and retain an interface appropriate to its platform.

## Current baseline

The web companion already provides:

- Compose Multiplatform/Wasm screens for Today, Calendar, Schedules, and Progress
- English/Hebrew presentation, RTL layout, Hebrew font support, and bilingual references
- Browser-local persistence, an installable PWA manifest, and an offline service worker
- Basic schedule creation, a short generated plan, completion, archive, progress, and JSON backup
- Optional Supabase magic-link accounts with manual revision-checked snapshot synchronization

The principal gap is architectural as well as functional. The web state currently contains only simplified schedules, tasks, and UI language. Android has a substantially richer Room model for material units, schedule rules, lifecycle history, exclusions, completion metadata, goals, and settings. Synchronizing the reduced web snapshot cannot provide full round-trip parity without discarding or inventing information.

The existing snapshot sync remains supported during migration, but it is a bridge rather than the target architecture.

## Priority and dependency order

| Priority | Milestone | Depends on | Primary result |
| --- | --- | --- | --- |
| P0 | 0. Baseline and contracts | — | A testable definition of parity |
| P0 | 1. Canonical data and offline sync | 0 | Lossless cross-device data |
| P0 | 2. Shared planning engine and catalogs | 1 | Identical schedules on both platforms |
| P1 | 3. Schedule workflow parity | 2 | Full creation and management on web |
| P1 | 4. Daily work and calendar parity | 1–3 | Equivalent operational workflow |
| P2 | 5. Progress, goals, settings, and portability | 1–4 | Equivalent history and utilities |
| P2 | 6. Web platform integrations | 1, 4 | Practical reminders and installability |
| P0 | 7. Hardening and parity release | all | Safe production rollout |

Milestones 1 and 2 are the critical path. Building the remaining screens first would create duplicate logic and another data migration.

## Milestone 0 — baseline and contracts

Indicative effort: 1 week

### Deliverables

- Add a maintained Android-to-web capability matrix, with each item marked `shared`, `web`, `Android-only`, or `planned`.
- Amend `PRODUCT_SPEC.md` to recognize optional accounts and cross-device sync while preserving offline-first behavior.
- Document stable domain terminology, IDs, date/time-zone semantics, ordering, and conflict rules.
- Capture representative Android data fixtures: each schedule type, completed and overdue tasks, exclusions, pauses, goals, preferences, and Hebrew references.
- Record the current web local-storage and cloud snapshot formats as legacy schema version 1.
- Define browser support and accessibility targets.

### Exit criteria

- Every Android capability has a disposition and an acceptance test or explicit platform exception.
- Golden fixtures can be loaded by automated JVM and Wasm tests.
- No unresolved product decision blocks the canonical data model.

## Milestone 1 — canonical data model and offline sync

Indicative effort: 2–3 weeks

This milestone is the release blocker for all subsequent parity work.

### Shared model

Move serialization-safe, platform-neutral representations into `shared/commonMain` for:

- schedules, material units, tasks, completion metadata, goals, exclusions, and pause intervals
- pace versus finish-by rules, missed-work behavior, chazarah rules, lifecycle state, and generation revision
- UI language, sefarim language, calendar display, reminder preference, and planning defaults
- preset identity/version and structured bilingual material coordinates
- stable UUIDs, creation/update timestamps, record revision, and deletion tombstones

Keep Room entities and browser persistence records as adapters around this model rather than separate definitions of the product.

### Browser persistence

- Replace the single local-storage payload with an IndexedDB database and explicit schema migrations.
- Add a durable local mutation outbox, last-applied cloud cursor, and tombstones.
- Retain a small local-storage bootstrap only for safe startup metadata when helpful.
- Import existing web schema-version-1 data once, validate it, and preserve a recoverable pre-migration export.

### Supabase sync

- Replace whole-snapshot synchronization with entity-level upserts/deletes or an append-only mutation log.
- Scope every record to the authenticated user and enforce row-level security for every table and RPC.
- Use server-assigned monotonic cursors for incremental pull; make pushes idempotent by mutation ID.
- Define deterministic conflicts per field/entity. Never use last-write-wins for destructive schedule regeneration or completion history without an explicit rule.
- Model completion/undo as revisioned state or events so edits made offline on different devices converge predictably.
- Keep account use optional. Signing out must leave a usable local copy, and authentication failure must never clear local data.
- Provide migration from the current `learning_snapshots` row and maintain a rollback window.

### Required conflict rules

| Concurrent change | Resolution |
| --- | --- |
| Completion vs unrelated schedule edit | Preserve both |
| Completion vs undo | Highest server revision; surface a recent-conflict notice when devices disagree |
| Archive/pause vs task completion | Preserve completion and lifecycle change |
| Delete vs stale offline edit | Tombstone wins; offer local export before discarding unpublished work |
| Future regeneration vs completion | Completed and historical tasks are immutable; regenerate only eligible unfinished future work |
| Preference changes | Per-field newest revision, not whole-settings replacement |

### Exit criteria

- Android, two browser profiles, and an installed PWA can edit offline and converge after reconnecting.
- Sync is incremental, retry-safe, and never requires replacing an entire device copy for ordinary conflicts.
- Migration tests cover Android’s current database/backup versions and the current web/local cloud snapshot.
- A failed migration or sync leaves the prior local database readable and exportable.

## Milestone 2 — shared planning engine and material catalogs

Indicative effort: 2–4 weeks

### Deliverables

- Move or adapt Android’s deterministic schedule engine into `shared/commonMain`; remove the web-only 14-assignment generator.
- Share structured material coordinates, ordered units, preset metadata, bilingual formatting, Hebrew numerals, and Ashkenazi transliteration rules.
- Support pace and finish-by distribution, selected weekdays, exclusions, pauses, and both missed-work behaviors.
- Support finite offsets, annual chazarah, leap-day behavior, Friday/Shabbos patterns, and the official Oraysa pattern.
- Share all current preset definitions and worldwide-position calculations, including signed catalog version metadata.
- Use an injectable clock, time zone, and “today” boundary at platform edges.

### Verification

- Run the same golden scheduling cases on JVM and Wasm.
- Compare generated task identity, source assignment, dates, bilingual labels, review sequence, and generation revision.
- Include DST/time-zone changes, leap years, empty weekday selection, uneven finish-by distribution, collisions, pauses, exclusions, and preset boundaries.

### Exit criteria

- Given the same canonical input, Android and web produce semantically identical plans.
- No schedule-generation business rule remains implemented only in a UI module.
- Existing Android schedules can be displayed and safely edited on web without degrading metadata.

## Milestone 3 — schedule workflow parity

Indicative effort: 2–3 weeks

### Deliverables

- Reproduce the localized first-run and first-schedule paths on web.
- Add every current preset: Daf Yomi Bavli, Oraysa, Dirshu Amud Yomi, Mishnah Yomis, Dirshu Mishnah Berurah, both Yerushalmi cycles, Rambam, Chofetz Chaim, Tehillim, Hachzek Pele Yoetz, and Kitzur Shulchan Aruch Yomi.
- Add current-position and dated earlier-position selection.
- Add structured custom material flows for Gemara, Mishnah, Mishnah Berurah, Kitzur, and Other.
- Support custom names/labels, exact ranges, pace or finish-by, weekdays, missed-work behavior, exclusions, and chazarah overrides.
- Show an accurate pre-save completion date and chazarah workload preview.
- Add future-only edit/regeneration, pause/resume, archive/restore, separate past-learning and past-chazarah bulk completion, and confirmed permanent deletion.

### Exit criteria

- Each Android schedule fixture can be created, inspected, edited, and managed from web.
- Preview and saved output match Android golden results.
- Edits preserve completed and historical work across clients.

## Milestone 4 — Today and Calendar parity

Indicative effort: 2 weeks

### Today

- Match schedule grouping and independently collapsible New Learning, Chazarah Due Today, Overdue Learning, and Overdue Chazarah sections.
- Keep past tasks completed today visible separately from still-overdue work.
- Apply persistent per-section sorting, bilingual reference preferences, due-state labels, and individual completion/undo.
- Re-evaluate the local day on resume, visibility change, midnight, and time-zone change.

### Calendar

- Add the full Gregorian/Hebrew month grid, per-day status, counts, unrestricted navigation, and selected-day details.
- Add schedule and learning/chazarah filters.
- Permit individual completion/undo for past and future tasks with the same semantics as Android.

### Exit criteria

- The same account shows the same grouped tasks, dates, filters, and completion state on Android and web.
- Today/calendar tests cover early completion, undo, overdue ordering, rollover, exclusions, and Hebrew dates.
- Keyboard, screen-reader, large-text, high-contrast, LTR, and RTL checks pass on supported browsers.

## Milestone 5 — Progress, goals, settings, backup, and print

Indicative effort: 1–2 weeks

### Deliverables

- Match completion percentage; learning/chazarah totals; unit totals; current/longest scheduled-day streaks; milestones; goals; and 30-day chazarah workload.
- Sync UI language, sefarim display language, primary calendar display, default chazarah, reminder settings, and preset-update preferences where meaningful.
- Introduce one cross-platform, versioned backup contract containing all canonical records and preferences.
- Import existing Android and web backups, with validate-before-replace and transactional restore behavior.
- Generate the same 7/14/30/60/90-day printable content using browser print/PDF, including localization, bilingual references, and checkboxes.

### Exit criteria

- Android and web calculate identical statistics from the same fixture.
- A full backup can round-trip Android → web → Android without data loss.
- Printed output passes English, Hebrew/RTL, pagination, and A4 checks.

## Milestone 6 — web platform integrations

Indicative effort: 1–2 weeks, with notification work dependent on deployment choices

### Deliverables

- Harden PWA installation, service-worker updates, offline startup, asset caching, and recovery from an interrupted update.
- Add best-effort browser notifications where platform support permits.
- If reminders must arrive when the web app is closed, add opt-in Web Push backed by Supabase/server scheduling; document browser and iOS limitations clearly.
- Refresh due counts while the app is open and use an app badge when supported.
- Reuse signed preset catalogs with cache validation and bundled fallback.

### Explicit platform exceptions

- The Android home-screen widget has no exact cross-browser equivalent. The web target is an installable PWA with app badges and a fast Today launch.
- Android’s WorkManager/AlarmManager reminder guarantees cannot be reproduced by a dormant browser tab. Reliable closed-app reminders require Web Push and user/browser permission.
- Android’s Storage Access Framework becomes browser download/upload flows.

These are accepted platform differences, provided the core data and workflow remain equivalent.

## Milestone 7 — hardening and parity release

Indicative effort: 2 weeks

### Quality gates

- Automated common-domain tests run for JVM and Wasm in continuous integration.
- Sync integration tests cover two-device offline changes, retries, duplicates, conflicts, deletes, account switching, and token expiry.
- Migration tests cover every supported Android database/backup and web IndexedDB/local-storage version.
- Browser matrix covers current Chrome, Edge, Firefox, and Safari, plus installed PWA behavior where supported.
- Accessibility review covers keyboard-only use, focus order, landmarks, screen readers, 200% zoom, contrast, reduced motion, and Hebrew RTL.
- Security review covers RLS, RPC authorization, public-key handling, redirect allowlists, session storage, dependency audit, and account deletion/export.
- Performance budgets cover cold load, IndexedDB startup, large histories, month navigation, and incremental sync.
- Service-worker and database upgrades have rollback/recovery tests.

### Rollout

1. Internal accounts and synthetic fixtures.
2. Opt-in beta with dual-read verification and downloadable safety backups.
3. Gradual default enablement of entity sync while retaining legacy snapshot recovery.
4. Remove legacy writes only after a measured migration window and verified restore path.

### Final parity gate

- All non-platform-specific Android capabilities have a passing web acceptance test.
- Cross-device edits converge without silent loss in the full conflict matrix.
- Android and web share the canonical model, planning rules, fixtures, and backup contract.
- Both clients remain fully usable for normal learning work while offline.
- No known destructive migration, regeneration, restore, or sync defect remains.

## Suggested releases

| Release | Includes | User-visible promise |
| --- | --- | --- |
| Web 0.2 — Safe Sync Core | Milestones 0–1 | Full-fidelity, recoverable cross-device data |
| Web 0.3 — Shared Planner | Milestone 2 | Android-identical schedule generation |
| Web 0.4 — Workflow Parity | Milestones 3–4 | Full schedule, Today, and Calendar workflows |
| Web 0.5 — Insights and Portability | Milestone 5 | Matching progress, goals, backup, and print |
| Web 1.0 — Parity | Milestones 6–7 | Production-quality web companion |

For one engineer, the full sequence is approximately 10–16 focused engineering weeks. The range is driven mainly by migration complexity, cross-device conflict behavior, and reliable Web Push. Web 0.4 is the first sensible “daily-driver parity” target; Web 1.0 adds platform integrations and production hardening.

## First implementation slice

Start with a narrow end-to-end vertical slice before expanding the schema:

1. Define canonical schedule, task, completion, settings, revision, and tombstone records in `shared`.
2. Add IndexedDB schema version 1 plus import of the existing local-storage payload.
3. Add Supabase entity tables/RLS and an idempotent mutation outbox protocol.
4. Migrate one existing schedule and its tasks from Android and web.
5. Prove offline completion, undo, schedule rename, and delete convergence across Android and two browsers.
6. Turn those scenarios into permanent integration tests.

Do not begin the full web schedule wizard until this slice passes. It validates the hardest assumptions while the model and cloud schema are still inexpensive to change.
