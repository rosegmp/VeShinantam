# Android–web parity matrix

This matrix is the acceptance inventory for `WEB_PARITY_ROADMAP.md`. “Partial” means the web surface exists but does not yet carry Android’s full model or behavior.

| Capability | Android | Web | Target / acceptance note |
| --- | --- | --- | --- |
| Today navigation and task completion | Complete | Complete | Five-section grouping, completed-today history, completion metadata/undo, collapsible sections, synchronized ordering, and browser-local day/time-zone re-evaluation |
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
| Cross-device sync | Incremental client enabled | Incremental client enabled | Both use the current entity API; full two-device conflict, retry, migration, and token-expiry integration verification remains in milestone 7 |
| Backup and restore | Complete | Complete | New exports use one validated canonical format; Android and web import canonical files plus their legacy Android/web formats before confirmed replacement |
| Printable schedule | Complete | Complete | Matching 7/14/30/60/90-day content rules with localized browser print/PDF, bilingual references, active learning/chazarah tasks, checked completion state, RTL, and A4 pagination |
| Reminders | Complete | Active-tab notification | Preference/time synchronize; permission is requested from a user action and the open web app schedules service-worker notifications. Closed-app delivery still requires Web Push |
| Home-screen presence | Android widget | Installable PWA + due badge | Manifest Today shortcut and active incomplete due-count app badge where the browser/OS supports it |
| Signed preset updates | Complete | Complete | Same HTTPS envelope/key ID, pinned P-256 signature verification, strict reference validation, atomic browser cache replacement, and bundled fallback |
| Light/dark mode | Complete | Complete | Both clients follow the system preference with matching accessible blue/gold schemes and semantic surface/status colors |
| Accessibility hardening | Complete | Partial | Keyboard roles/state for navigation, task completion, collapsible sections, and calendar cells plus reduced-motion support are implemented; formal screen-reader, focus-order, 200% zoom, contrast, and RTL browser validation remains |
| Release/migration verification | Complete | Partial | JVM/Wasm, Android, and web tests now gate deployment in CI; browser matrix, IndexedDB/service-worker recovery, and sync integration tests remain |

## Platform exceptions

- A dormant browser cannot provide Android-equivalent local scheduled execution. Closed-app reminders require opt-in Web Push and remain subject to browser/OS policy.
- Browsers do not expose an Android-style home-screen widget. The supported web equivalent is an installable PWA, app badge where available, and a direct Today launch.
- Browser download/upload replaces Android’s system file-picker integration.

No exception permits loss of canonical learning data or different schedule-generation results.
