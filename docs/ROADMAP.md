# VeShinantam — implementation roadmap

Development proceeds in small vertical increments. “MVP complete” means the product acceptance criteria in `PRODUCT_SPEC.md` are satisfied, not merely that screens exist.

## Current foundation increment

- [x] Standalone Gradle/Android application structure
- [x] Kotlin + Jetpack Compose + Material 3 theme baseline
- [x] Deep-blue/warm-gold brand tokens and automatic system light/dark behavior
- [x] English and Hebrew resources with RTL support enabled
- [x] Four-destination navigation shell and Today-first launch
- [x] Accessible, bilingual sample Today task cards with individual completion toggles
- [x] Practical domain types for schedule/task/chazarah rules
- [x] Pure Kotlin scheduling engine for eligible-day shifting, even distribution, finite offsets, and annual recurrence horizon
- [x] Unit tests for core scheduling invariants
- [x] Room schema/DAO/repository foundation for offline schedules and tasks
- [x] Notification permission/channel and reminder-worker foundation
- [x] Resolve/build all external Android dependencies in this environment and run the full test suite

## MVP increment 1 — persistent Today loop

- [x] Replace sample state with Room-backed flows and transactional individual completion/undo
- [x] Implement current-time-zone task-day refresh on app resume
- [x] Implement exact required Today grouping/order and completed-today visibility
- [x] Persistent per-section Today sorting with independently collapsible schedules and sections
- [x] Seed development fixtures only in debug builds
- [x] Add a Room migration and persist completion instant, local date, and zone
- [x] Add midnight wakeup while the app remains continuously foregrounded
- [ ] Tests: DAO, repository, early completion, and undo invariants (pure grouping/order test is complete)

## MVP increment 2 — first schedule onboarding

- [x] One-time localized welcome with language choice and explicit wizard, direct setup, or skip paths
- [x] Seven-step localized first-schedule wizard with back navigation and a separate one-time welcome
- [x] Bundled versioned catalogs for the five first-release presets
- [x] Bundled worldwide positions with explicit “catalog as-of” state and earlier-position selection
- [x] Structured custom sefer choice: Gemara, Mishnah, Mishnah Berurah, Kitzur, or Other
- [x] Ordered from/to Gemara and Mishnah ranges with pre-populated unit lists, Mishnah mishnah/perek choice, Mishnah Berurah unit-specific page/seif/siman catalogs, Kitzur simanim, and Gemara daf/amud choice
- [x] One-at-a-time custom unit entry using a single field that accepts English or Hebrew
- [x] Daily-pace and finish-by generation, date selection, and weekday selection for custom schedules
- [x] Per-schedule missed-work choice and chazarah pattern override for custom schedules
- [x] Official Oraysa daily and Friday/Shabbos chazarah with optional additional user-defined reviews
- [x] Preview completion date and future chazarah counts before commit
- [x] Transactional schedule/task creation with immediate chazarah materialization
- Tests: every preset boundary, uneven division, no selected-day edge case, bilingual reference snapshots

## MVP increment 3 — calendar and schedule management

- [x] Rolling Room-backed agenda with Gregorian and Hebrew dates
- [x] Schedule/type filters and future individual completion
- [x] Full month browser, day-status grid, and month-scoped queries through the complete materialized schedule
- [x] Pause/resume schedule lifecycle
- [x] Persisted per-schedule exclusions and automatic missed-learning rollover across app, reminder, and widget entry points
- [x] Future-only regeneration for pace/weekdays/start-date edits with completed and historical task preservation
- [x] Archive/restore and confirmed permanent deletion
- Tests: completed-history preservation, collisions, pause/exclusion shifts, overdue review behavior

## MVP increment 4 — progress and goals

- [x] Scheduled-day streak engine
- [x] Completion percentage and typed totals
- [x] Upcoming review workload visualization
- [x] Automatic milestones and badges for streaks, learning units, and chazarah
- [x] Optional completion/streak/unit goals
- Tests: rest/pause/exclusion streak skipping and unit aggregation

## MVP increment 5 — reminders and widget

- [x] Settings-backed single daily reminder
- [x] Correct learning/review count summary and Today deep link
- [x] Reboot/time/time-zone/package-replaced rescheduling receivers
- [x] RemoteViews home-screen widget showing remaining tasks
- [x] Refresh after completion and day rollover
- Tests plus device/emulator checks across notification permission and battery modes

## MVP increment 6 — portability and release hardening

- [x] Versioned backup serializer, validation, transactional restore, and Storage Access Framework flows
- [x] Printable localized PDF for a selected future-day count with one checkbox per task
- [x] Preset-update client with signed/versioned catalog validation and bundled fallback
- [x] Full English/Hebrew string audit; RTL, TalkBack semantics, large-font, contrast, and touch-target QA
- [x] Database migrations, restore compatibility fixtures, release signing/private-distribution documentation
- [x] End-to-end smoke test and reproducible release build

## MVP completion gate

- All acceptance criteria pass on a supported API range
- No network is required for normal operation
- No known destructive edit/regeneration bug
- Backup round-trip and migration fixtures pass
- English/Hebrew screenshots reviewed in light/dark and large-font modes
- Release APK/AAB generated with checksum and private install instructions

## Later enhancements (not required for MVP)

- More preset catalogs and richer structured material metadata
- User-selectable annual-review materialization horizon display
- Improved tablet/foldable two-pane layouts
- Widget size variants and richer glanceable workload visualization
- Optional encrypted backup container
- Optional device-to-device transfer initiated locally by the user
- Expanded milestone library and goal visualizations
- More robust catalog signature rotation/update diagnostics

The excluded social/account/sync/text features remain out of scope unless the product direction explicitly changes.
