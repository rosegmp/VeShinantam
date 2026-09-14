# VeShinantam — Product specification

Status: implementation baseline for Android and web

Platforms: Android and Compose Multiplatform Web, single-user and offline-first
Package: `app.veshinantam`

## 1. Product intent

VeShinantam helps one person plan Torah learning and chazarah, see exactly what is due, and mark each obligation complete. An optional account synchronizes that person's data across their devices while each device remains useful offline. The product deliberately avoids social mechanics, messaging, rankings, leader dashboards, and embedded sefer text.

Success means that the app remains dependable without a network connection, makes both today's work and future workload legible, preserves history when plans change, and works naturally in English, Hebrew, LTR, RTL, light mode, dark mode, large text, and TalkBack.

## 2. Product principles

1. **The plan is trustworthy.** Dates are deterministic, based on local calendar dates rather than elapsed hours. A new task day begins at local midnight and uses the phone's current time zone.
2. **Every obligation is explicit.** New learning and every chazarah occurrence are separate tasks with their own completion state. There is no bulk-complete action.
3. **History does not move.** Completed tasks remain unchanged when a schedule is edited. Only unfinished future work is regenerated.
4. **Offline is normal.** Creation, planning, completion, history, reminders, widgets, PDF generation, backup, and restore use on-device data. Network access is optional and used for account sync and preset-data updates.
5. **Structured Torah references remain bilingual.** Preset and structured references supplied by the app show English and Hebrew together. User-entered schedule names and custom unit labels are single-field values entered in either language and displayed verbatim.
6. **Calm, native presentation.** Material 3 behavior and accessibility come first. Deep blue and restrained warm-gold details provide identity without ornament competing with the work.

### Transliteration convention

English transliteration uses conventional Ashkenazi pronunciation throughout user-facing copy and generated references—for example, **Berachos**, **Shabbos**, **Chazarah**, **Mishnayos**, **Perakim**, and **Simanim**. Familiar forms that do not materially differ in common Ashkenazi usage remain unchanged, including **Torah**, **Gemara**, **Daf**, **Amud**, **sefer**, **Mishnah Berurah**, and **Kitzur Shulchan Aruch**. Official preset identifiers may retain a provider's canonical name internally, but the displayed label follows this convention (for example, **Mishnah Yomis**).

## 3. Users and scope

### Primary user

An individual maintaining one or more parallel learning programs, who may join a worldwide cycle, catch up from an earlier position, create a personal program, and use spaced chazarah.

### Explicitly out of scope

- Shared/family accounts, collaboration, or social synchronization
- Groups, leaderboards, rankings, messaging, social sharing
- Notes, recall ratings, partial credit, elapsed-time tracking
- Actual Gemara, Mishnah, halachah, or other sefer text
- CSV or multiline bulk import
- Readable progress-report export
- Heavy-workload warnings
- An in-app light/dark theme switch

## 4. Information architecture

Bottom navigation contains:

- **Today** — default destination and action surface
- **Calendar** — browse all past and future assignments
- **Schedules** — create and manage programs
- **Progress** — history, statistics, goals, and motivation

The top app menu contains Settings, Backup & restore, and Export printable schedule.

## 5. Core domain model

### Schedule

- Stable ID and one user-facing display name; custom names may be entered in either English or Hebrew
- Kind: preset or custom
- Material type: daf, amud, mishnah, perek, page, siman, or custom unit
- Preset ID and bundled-data version when applicable
- Starting material position and ordinal
- Local start date
- Optional target completion date
- Pace quantity and pace unit
- Selected weekdays
- Missed-work behavior: `SHIFT_FORWARD` or `KEEP_FIXED_OVERDUE`
- Chazarah pattern: inherit global defaults or schedule override
- Excluded local dates
- State: active, paused, archived
- Pause intervals and timestamps

### Material unit

- Stable ID and schedule ID
- Ordered ordinal
- One user-entered label for custom units, in either English or Hebrew; app-supplied structured references retain paired English/Hebrew data
- Optional structured coordinates (tractate/daf/side, masechta/perek/mishnah, siman, page)
- Unit-count contribution used by progress totals

### Task

- Stable ID and schedule ID
- Material unit ID or assignment range
- Type: new learning or chazarah
- Planned local date
- Original learning date (identical to planned date for learning; source date for reviews)
- Review sequence/offset identity so coincident reviews never collapse
- Bilingual material label snapshot
- Completion timestamp and completion time-zone ID, nullable
- Generation revision

### Settings

- UI language: English or Hebrew
- Global chazarah pattern: defaults to 1, 7, 30, 180 days, then annual indefinitely
- Daily reminder enabled and local time
- Optional preset-update preference/version metadata

### Goal

- Optional schedule association
- Type: finish schedule by date, streak target, or completed-unit target
- Target value/date and completion state

## 6. Supported learning programs

Structured types:

- Gemara by daf or amud, with a flexible daily quantity
- Mishnayos by selected number of mishnayos or perakim
- Mishnah Berurah by page, seif, or siman
- Kitzur Shulchan Aruch by siman/structured unit
- Any sefer or topic using free-form units

Custom schedules support either:

1. A predefined ordered list of units entered one at a time, or
2. A daily quantity whose generated tasks receive manually entered labels.

The first bundled preset catalog contains Daf Yomi Bavli, Oraysa, Amud Yomi, Mishnah Yomis, and Dirshu Mishnah Berurah. A preset normally starts at the current worldwide position; the user may select any earlier position to catch up. Presets contain references and cycle metadata only, never sefer text.

Custom creation begins with a sefer category: Gemara, Mishnah, Mishnah Berurah, Kitzur Shulchan Aruch, or Other. Gemara and Mishnah use ordered From and To masechta selectors rather than independent multi-selection. Their start and end units are selected from pre-populated lists valid for the corresponding masechtos; Gemara chooses daf or amud, while Mishnah chooses individual mishnayos or whole perakim. Mishnah Berurah requires a chelek, then page, seif, or siman units selected from unit-specific pre-populated lists: printed pagination for Page, actual seif entries grouped by siman for Seif, and the chelek's siman range for Siman. Kitzur uses pre-populated siman lists. Other retains one-at-a-time manual unit labels.

## 7. Schedule-generation rules

### Inputs

The wizard collects program, position, start date, one of pace or completion date, selected weekdays, exclusions (optional during creation), missed-work behavior, and chazarah pattern.

### Eligible dates

A date is eligible when its weekday is selected, it is not excluded, and it is not inside a pause interval. If a computed assignment falls on an ineligible date, move it to the next eligible date.

### Pace mode

Starting at the selected position and date, assign up to the selected quantity to each eligible date until the material ends. The displayed completion date is the final generated learning date.

### Completion-date mode

Find all eligible dates from start through target date. Divide remaining atomic material units as evenly as possible. When division is uneven, assign one extra unit to the earliest eligible days, producing slightly larger assignments. If the material cannot be represented in the chosen unit granularity, retain the atomic-unit remainder in the final assignment.

### Chazarah generation

Generate review tasks immediately alongside every learning task, before any learning completion:

- Add each finite day offset to the original planned learning date.
- Move a result forward to the next eligible schedule date.
- After the 180-day review, generate annual reviews on the same Gregorian month/day for the supported planning horizon. Leap-day anniversaries use February 28 in non-leap years.
- Keep every obligation as a distinct row even if multiple offsets or source assignments produce the same schedule/date/material combination.
- Never recalculate review dates because of early/late completion or completion undo.

The on-device app maintains a finite materialized horizon for indefinite annual reviews and extends it during maintenance work; semantically the recurrence is indefinite.

### Missed work

- `SHIFT_FORWARD`: at day rollover, unfinished past learning is moved forward through eligible days while preserving order and avoiding destructive changes to completed history. Its already-created chazarah remains tied to its original planned date.
- `KEEP_FIXED_OVERDUE`: planned dates never move; unfinished tasks appear overdue.
- Missed reviews never shift and remain overdue until completed.

### Pause, exclusions, and editing

- Pausing records a start date and removes the schedule from due work. Resuming shifts unfinished learning forward by the number of eligible paused days; reviews already due remain distinct and unfinished reviews resume as overdue.
- Adding an excluded date shifts affected unfinished learning to the next eligible date.
- Editing pace, weekdays, or start date regenerates only unfinished learning whose planned date is today or later, and the future review tasks derived from those regenerated unfinished items. Completed tasks and historical items are immutable.
- Archive hides a schedule from active views but keeps all history. Permanent delete removes its schedule, tasks, units, goals, and exclusions after explicit confirmation.

## 8. Task-day and completion semantics

- Persist all due dates as ISO local dates, not instants.
- Resolve "today" from the current system clock and current phone time zone every time the app resumes and in scheduled/background entry points.
- Completion is binary: learned/not learned.
- Tapping an unchecked task marks only that task complete and records an instant. Undo clears only that task's completion fields.
- Future learning and review tasks may be opened and completed early.
- Overdue ordering is oldest planned date first, then stable schedule/order sequence.

## 9. Today grouping and order

Top-level groups are schedules/programs in user order. Within each schedule:

1. New Learning (due today)
2. Chazarah Due Today
3. Overdue Learning
4. Overdue Chazarah

Completed-today items remain visible in their group with checked state. Overdue groups sort oldest date first. Each row exposes one accessible checkbox/action, the bilingual material reference, and the due date when it is not today.

## 10. Calendar and progress

Calendar shows Gregorian date plus Hebrew date, completion state, and task counts. Users can move through months, select a day, and inspect or complete any individual past/future task. Filters allow schedule selection and new-learning/chazarah type.

Progress includes:

- Calendar/history view
- Current streak counted only across eligible scheduled days; rest days, exclusions, and pause dates are skipped rather than counted as failures
- Completed / scheduled percentage
- Totals by daf, amud, mishnah, perek, page, siman, and custom unit
- Upcoming chazarah workload
- Light milestones, streak badges, and optional completion goals

## 11. Notifications, widget, backup, and PDF

- One exact-or-best-effort daily reminder time summarizes all incomplete work currently due: for example, “3 learning tasks and 5 chazarah tasks due.” Tapping opens Today.
- Reminder scheduling is re-established after reboot, time-zone change, time/date change, app update, restore, or settings change using WorkManager/AlarmManager as appropriate to OS restrictions.
- The home-screen widget shows today's remaining total and a compact task list; tapping opens Today. It refreshes after completion and day changes.
- Backup exports one versioned local file containing schedules, material metadata, settings, goals, exclusions, tasks, and completion history. Import validates schema/version and offers replace-after-validation semantics.
- Printable PDF exports the next user-selected number of days, with a checkbox beside every task. UI labels follow the selected language while every Torah reference stays bilingual.

## 12. Localization, RTL, visuals, and accessibility

- All UI text is in Android string resources (`values` and `values-iw`/`values-he`), with plural resources for task counts.
- Layout uses start/end semantics. Hebrew changes app locale and layout direction to RTL; bilingual reference rows preserve readable directional isolates.
- System light/dark mode is followed via Material 3; no theme toggle exists.
- Brand palette: deep blue (`#173B67` light primary, accessible tonal variant in dark mode) and warm gold (`#C59636`) for restrained separators, milestones, and sefer/arrow icon detail.
- Typography uses system-scaled `sp`; no text is clipped at large font settings. Touch targets are at least 48dp.
- Icon direction: an open sefer silhouette with a subtle circular review arrow, readable as a monochrome adaptive icon.
- Every icon-only control has a content description. Checkboxes announce task type, bilingual reference, due state/date, and checked state. Color is never the only status signal.

## 13. Offline data and architecture

- Single-activity Kotlin application using Jetpack Compose and Material 3.
- Room is the source of truth. Repositories expose cold `Flow`s and perform transactional mutations.
- ViewModels own screen state and invoke use cases. Pure Kotlin schedule-generation and streak engines have deterministic unit tests using injected `Clock`/time zone.
- DataStore stores small preferences; settings that must participate in backup may be mirrored into the backup document.
- WorkManager performs resilient day rollover, reminder maintenance, preset-catalog updates, and recurrence-horizon extension. AlarmManager may deliver the user-selected reminder time where platform policy permits.
- Navigation Compose uses stable route arguments containing IDs rather than serialized objects.

## 14. Privacy and security

- Accounts are optional. With account sync disabled or signed out, no learning progress leaves the device; the product has no analytics, ads, or telemetry.
- Backups and PDFs leave the app only through the Storage Access Framework at explicit user request.
- Imported files are treated as untrusted: size limits, schema validation, enum/range validation, referential-integrity validation, and transactional replacement are required.
- Optional preset updates use signed/versioned catalogs over HTTPS and never include user progress.

## 15. Acceptance criteria for the first usable MVP

1. A fresh install opens onboarding and can create at least one preset or custom schedule.
2. Generation obeys selected weekdays/exclusions and produces distinct reviews for 1/7/30/180-day offsets plus a materialized annual horizon.
3. Today displays the required groups and lets every item be completed/undone independently, including future items.
4. Multiple schedules coexist; pause, archive, and future-only regeneration preserve completed history.
5. Calendar exposes past and future tasks with bilingual references.
6. Progress calculates scheduled-day streak and unit totals correctly.
7. English/Hebrew, RTL, system theme, large fonts, and TalkBack labels pass manual checks.
8. Daily reminder and widget reflect remaining due counts.
9. Backup round-trip restores all local state; printable PDF contains dated tasks and checkboxes.
10. Unit tests cover distribution, eligibility shifting, review identity, leap-day annual reviews, streak exclusions, and regeneration preservation.
