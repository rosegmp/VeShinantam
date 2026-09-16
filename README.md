# VeShinantam

VeShinantam is a standalone, private, offline-first Android app for planning Torah learning and chazarah. This repository is an early native MVP foundation built with Kotlin, Jetpack Compose, Material 3, Room, and WorkManager.

The repository also contains a Kotlin/Compose Multiplatform Web companion. Android and web can use the same optional Supabase account with email/password authentication, preserve device-local offline data, and require an explicit choice before resolving divergent cloud and device copies.

The web Today view now mirrors Android's separate new-learning, chazarah-due-today, overdue-learning, overdue-chazarah, and completed-today sections. Sections are collapsible, overdue work shows its due date, and the synchronized sort preference supports scheduled, newest-due, and natural reference ordering in English or Hebrew. The browser re-evaluates the local date after midnight, resume, and time-zone changes without requiring a reload.

The web Calendar grid shows filter-aware incomplete, partial, and complete status for each day, including completed/total counts. Every cell includes its Hebrew day number, selected dates show full Gregorian and Hebrew dates, and schedule/type filters update both the grid and day details.

The web app is deployed automatically from `main` to [GitHub Pages](https://rosegmp.github.io/VeShinantam/).

## What is implemented now

- A four-tab Compose application shell with Today as the default screen
- Branded automatic light/dark color schemes
- English and Hebrew resources with RTL support
- Persistent in-app English/Hebrew interface switching with immediate RTL/LTR refresh, also applied to widgets and notifications
- An independent English, Hebrew, or bilingual sefarim-display preference used for learning references across Today, Calendar, material selectors, and the widget; schedule names remain single-language
- Hebrew sefer references use Hebrew-letter numbering, including daf/amud notation such as `ברכות דף ב.`
- Consistent Ashkenazi transliteration in user-facing English references
- Room-backed Today tasks grouped into expandable sections, with past tasks completed today separated from overdue work
- Persistent, individually completable bilingual tasks with completion/undo history
- Room entities, DAO, versioned schema/migration, repository, and debug-only fixtures
- Custom schedule creation with single-language names and unit labels, start/finish dates, pace or finish-by planning, weekdays, missed-work behavior, optional chazarah overrides, preview, and save
- Offline preset programs for Daf Yomi Bavli, Oraysa, Dirshu Amud Yomi, Mishnah Yomis, Dirshu Mishnah Berurah, Yerushalmi Yomi (Vilna), Yerushalmi Yomi (Schottenstein), Rambam, Chofetz Chaim, Tehillim, Hachzek Pele Yoetz, and Kitzur Shulchan Aruch Yomi, with versioned worldwide positions and dated earlier-position selection
- Optional Friday/Shabbos chazarah for every preset and custom schedule, with Oraysa retaining its official daily and weekend pattern; additional interval chazaros remain independently configurable
- Structured custom material selection for Gemara, Mishnah, Mishnah Berurah, Kitzur, or Other; includes ordered from/to selectors, pre-populated unit lists, exact Gemara daf/amud endpoints, Mishnah mishnah/perek choice, and two-sided Mishnah Berurah page plus seif/siman choices backed by unit-specific structures
- Room-backed Calendar month browser with Gregorian/Hebrew dates, completion-status grid, schedule/type filters, unrestricted month navigation, and individual completion
- Schedule lifecycle management with future-only pace/weekday/start-date editing, pause, resume, archive, restore, separate confirmed bulk completion of past learning or chazarah, and confirmed permanent deletion
- Room-backed Progress overview with completion percentage, learning/chazarah totals, current and longest scheduled-day streaks, milestone badges, optional locally persisted goals, and a 30-day chazarah workload
- A pure scheduling engine and unit tests for pace/date distribution and chazarah rules
- Settings-backed daily aggregate reminders at a user-selected local time, with correct learning/chazarah counts, a Today-screen notification link, and automatic rescheduling after reboot, clock/time-zone changes, and app updates
- A localized home-screen widget listing today’s tasks and separately counting overdue learning and chazarah, refreshed after changes and at day rollover
- Versioned JSON backup and transactional restore through Android’s system file picker, covering schedules, tasks, completion history, exclusions, goals, and user preferences
- Optional Supabase account with a durable per-entity outbox, revisions, tombstones, deterministic conflict handling, and automatic network-constrained retries; rollout remains gated until the matching Android/web release
- Printable A4 schedule PDFs for the next 7, 14, 30, 60, or 90 days, localized for English/Hebrew and the selected sefarim and primary-calendar display, with one checkbox per task
- Optional HTTPS preset-position updates with a pinned ECDSA signature, monotonic rollback protection, strict bundled-reference validation, atomic private caching, daily WorkManager checks, and automatic bundled fallback
- Exported Room schemas with on-device upgrade tests for every database version, plus committed compatibility fixtures for every supported backup format
- Repeatable device smoke and release-build scripts with clean-build reproducibility and SHA-256 verification
- Complete product specification, wireframes, and implementation roadmap in `docs/`

Schedules supports preset and custom-plan creation, Calendar provides a full month browser, and Progress summarizes live Room data. See `docs/ROADMAP.md` for the precise MVP boundary.

## Build

Prerequisites: Android Studio with Android SDK 36, JDK 17+, and internet access for the first dependency resolution. The app supports Android 7.0 (API 24) and newer.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Open this directory directly in Android Studio. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

See `docs/RELEASE.md` for release-key setup, signed builds, verification, and private-distribution guidance.

## Project map

- `app/src/main/java/app/veshinantam/domain` — deterministic domain model and scheduling rules
- `app/src/main/java/app/veshinantam/data` — offline Room foundation
- `app/src/main/java/app/veshinantam/ui` — Compose app shell and theme
- `app/src/main/java/app/veshinantam/notifications` — persisted daily reminder settings, scheduling, and aggregate notifications
- `app/src/test` — pure JVM scheduling tests
- `app/src/androidTest` — dependency-free on-device migration and end-to-end data-path smoke tests
- `scripts` — device smoke and reproducible release packaging
- `docs` — product specification, wireframes, Android roadmap, [web parity roadmap](docs/WEB_PARITY_ROADMAP.md), and [parity matrix](docs/WEB_PARITY_MATRIX.md)

No code or assets are shared with any other project.
