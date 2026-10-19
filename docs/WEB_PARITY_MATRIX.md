# Android–web parity matrix

This matrix is the acceptance inventory for `WEB_PARITY_ROADMAP.md`. “Partial” means the web surface exists but does not yet carry Android’s full model or behavior.

| Capability | Android | Web | Target / acceptance note |
| --- | --- | --- | --- |
| Today navigation and task completion | Complete | Complete | Five learning-status sections, completed-status and schedule grouping, completed-today history, completion metadata/undo, independently collapsible groups and schedules, synchronized ordering, and browser-local day/time-zone re-evaluation. Group and expansion choices remain local to each device. |
| Calendar | Complete | Complete | Gregorian/Hebrew month navigation, filter-aware statuses/counts, bilingual labels/details, schedule/type filters, and individual completion/undo |
| Schedule list and archive | Complete | Complete | Pause/resume, archive/restore, confirmed deletion, separately confirmed past-learning/past-chazarah completion, and future-only editing/regeneration preserve history and completed work |
| Preset creation | Complete | Complete | All 12 Android presets share exact bilingual units, positions, dates, cadence/exclusions, earlier-position search, and generated output |
| Structured custom schedules | Complete | Complete | Shared exact-range selectors, pace/finish-by, weekdays, synchronized exclusions, missed-work behavior, and pre-save workload preview |
| Chazarah planning | Complete | Complete | Finite offsets, annual reviews, weekend ranges, and official Oraysa behavior use the shared planner |
| Deterministic planning engine | Complete | Complete | One `shared/commonMain` implementation with JVM/Wasm golden tests enforced in CI |
| Progress statistics and streaks | Complete | Complete | Shared due-through-today totals, learning quantities, reviews, current/longest streaks, milestones, synchronized goals, and 30-day chazarah workload |
| English/Hebrew UI and RTL | Complete | Complete | Maintain browser accessibility and directional isolation |
| Sefarim display language | Complete | Complete | English, Hebrew, or both, independently from UI language; preference is backed up and synchronized |
| Preferences and settings | Complete | Complete | Language, reference language, primary calendar, default chazarah, Today sort, reminder time/state, and preset-update preference round-trip and synchronize without clobbering platform-specific behavior |
| Hebrew references/numerals | Complete | Complete | One shared JVM/Wasm formatter normalizes Hebrew numerals and legacy Daf wording across Android, web task views, readers, and printable schedules; cross-platform reference snapshots gate regressions |
| Offline persistence | Room | IndexedDB + installable offline shell | IndexedDB authoritative store with migrations/outbox; atomic service-worker upgrades pre-cache both Wasm runtimes and preserve the last working shell after interrupted updates |
| Optional account | Complete | Complete | Keep magic-link flow and local use without an account |
| Cross-device sync | Incremental client enabled | Incremental client enabled | Both use the current entity API and protect account binding and pending local mutations; CI covers two independent browser stores converging through offline edits, tombstones, remote-wins conflicts, interrupted acknowledgements, idempotent replay, and late-ack replacement rebasing. Web session tests cover refresh boundaries, rotating-token persistence, concurrent refresh coalescing, malformed responses, and retry after failure. The user confirmed a live Android-to-web account test on 2026-10-16. |
| Backup and restore | Complete | Complete | New exports use one validated canonical format; Android and web import canonical files plus their legacy Android/web formats before confirmed replacement |
| Printable schedule | Complete | Complete | Matching 7/14/30/60/90-day content rules with localized browser print/PDF, bilingual references, active learning/chazarah tasks, checked completion state, RTL, and A4 pagination |
| Mishnah Berurah scan reader | Local PDF | Local PDF | Each device selects its own scan, aligns a PDF page to an assignment, and opens later assignments at the corresponding page. The PDF is stored locally and is excluded from account sync and backups. |
| Reminders | Complete | Web Push + active-tab fallback | Preference/time synchronize. Signed-in browsers with permission subscribe to server-scheduled Web Push, with due counts computed from the latest synced active schedules and incomplete tasks in each browser time zone. Users must sync after changes to keep closed-app reminders current. A browser without an account uses an active-tab notification. Delivery remains subject to browser/OS notification policy. |
| Home-screen presence | Android widget | Installable PWA + due badge | Manifest Today shortcut and active incomplete due-count app badge where the browser/OS supports it |
| Signed preset updates | Complete | Complete | Same HTTPS envelope/key ID, pinned P-256 signature verification, strict reference validation, atomic browser cache replacement, and bundled fallback |
| Light/dark mode | Complete | Complete | Both clients follow the system preference with matching accessible blue/gold schemes and semantic surface/status colors |
| Accessibility hardening | Complete | Partial | Keyboard roles/state for navigation, task completion, collapsible sections, and calendar cells plus reduced-motion support are implemented; formal screen-reader, focus-order, 200% zoom, contrast, and RTL browser validation remains |
| Release/migration verification | Complete | Partial | JVM/Wasm, Android, web, entity-sync policy, token-refresh policy, deterministic IndexedDB bootstrap/migration/write-failure recovery, atomic service-worker upgrade/recovery, production bundle-size checks, and Chrome/Firefox Wasm suites gate deployment in CI; Edge/Safari and installed-PWA validation, real-browser destructive-storage recovery, and live two-device refresh tests remain |

## Platform exceptions

- Closed-app reminders use opt-in Web Push and require an account, notification permission, and a browser/OS that supports Push API delivery.
- Browsers do not expose an Android-style home-screen widget. The supported web equivalent is an installable PWA, app badge where available, and a direct Today launch.
- Browser download/upload replaces Android’s system file-picker integration.

No exception permits loss of canonical learning data or different schedule-generation results.
