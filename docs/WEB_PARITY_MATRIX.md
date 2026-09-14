# Android–web parity matrix

This matrix is the acceptance inventory for `WEB_PARITY_ROADMAP.md`. “Partial” means the web surface exists but does not yet carry Android’s full model or behavior.

| Capability | Android | Web | Target / acceptance note |
| --- | --- | --- | --- |
| Today navigation and task completion | Complete | Partial | Same grouping, ordering, history, completion timestamp, and undo |
| Calendar | Complete | Partial | Gregorian/Hebrew month, statuses, filters, and completion |
| Schedule list and archive | Complete | Partial | Full lifecycle, pause/resume, restore, delete, and bulk past completion |
| Preset creation | Complete | Partial | All Android presets, positions, dates, and exact generated output |
| Structured custom schedules | Complete | Partial | Same selectors, pace/finish-by, weekdays, exclusions, missed-work, and preview |
| Chazarah planning | Complete | Partial | Finite, annual, weekend, and official Oraysa behavior |
| Deterministic planning engine | Complete | Partial | One `shared/commonMain` implementation with JVM/Wasm golden tests |
| Progress statistics and streaks | Complete | Partial | Identical totals, streaks, milestones, goals, and workload |
| English/Hebrew UI and RTL | Complete | Complete | Maintain browser accessibility and directional isolation |
| Sefarim display language | Complete | Missing | English, Hebrew, or both, independently from UI language |
| Hebrew references/numerals | Complete | Partial | Shared formatter and reference snapshots |
| Offline persistence | Room | IndexedDB foundation | IndexedDB authoritative store with migrations and outbox |
| Optional account | Complete | Complete | Keep magic-link flow and local use without an account |
| Cross-device sync | Snapshot/manual | Snapshot/manual | Incremental entity sync, retries, tombstones, and deterministic conflicts |
| Backup and restore | Complete | Partial | One canonical format and lossless Android ↔ web round trip |
| Printable schedule | Complete | Missing | Equivalent browser print/PDF output |
| Reminders | Complete | Missing | Best-effort notification; Web Push for closed-app delivery if enabled |
| Home-screen presence | Android widget | Installable PWA | Accepted platform exception; add badges/fast Today launch where supported |
| Signed preset updates | Complete | Missing | Shared validated catalog and cached fallback |
| Light/dark mode | Complete | Partial | Follow system preference on web with matching brand tokens |
| Accessibility hardening | Complete | Partial | Keyboard, focus, screen readers, 200% zoom, contrast, and RTL |
| Release/migration verification | Complete | Partial | Browser matrix, IndexedDB/service-worker recovery, and sync integration tests |

## Platform exceptions

- A dormant browser cannot provide Android-equivalent local scheduled execution. Closed-app reminders require opt-in Web Push and remain subject to browser/OS policy.
- Browsers do not expose an Android-style home-screen widget. The supported web equivalent is an installable PWA, app badge where available, and a direct Today launch.
- Browser download/upload replaces Android’s system file-picker integration.

No exception permits loss of canonical learning data or different schedule-generation results.
