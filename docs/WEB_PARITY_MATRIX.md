# Android–web parity matrix

This matrix is the acceptance inventory for `WEB_PARITY_ROADMAP.md`. “Partial” means the web surface exists but does not yet carry Android’s full model or behavior.

| Capability | Android | Web | Target / acceptance note |
| --- | --- | --- | --- |
| Today navigation and task completion | Complete | Complete | Five-section grouping, completed-today history, completion metadata/undo, collapsible sections, synchronized ordering, and browser-local day/time-zone re-evaluation |
| Calendar | Complete | Partial | Filter-aware statuses/counts, bilingual day labels, selected-date details, unrestricted Gregorian navigation, filters, and completion are complete; primary Hebrew-month navigation remains |
| Schedule list and archive | Complete | Partial | Full lifecycle, pause/resume, restore, delete, and bulk past completion |
| Preset creation | Complete | Partial | All Android presets, positions, dates, and exact generated output |
| Structured custom schedules | Complete | Partial | Same selectors, pace/finish-by, weekdays, exclusions, missed-work, and preview |
| Chazarah planning | Complete | Partial | Finite, annual, weekend, and official Oraysa behavior |
| Deterministic planning engine | Complete | Partial | One `shared/commonMain` implementation with JVM/Wasm golden tests |
| Progress statistics and streaks | Complete | Partial | Identical totals, streaks, milestones, goals, and workload |
| English/Hebrew UI and RTL | Complete | Complete | Maintain browser accessibility and directional isolation |
| Sefarim display language | Complete | Complete | English, Hebrew, or both, independently from UI language; preference is backed up and synchronized |
| Hebrew references/numerals | Complete | Partial | Shared formatter and reference snapshots |
| Offline persistence | Room | IndexedDB foundation | IndexedDB authoritative store with migrations and outbox |
| Optional account | Complete | Complete | Keep magic-link flow and local use without an account |
| Cross-device sync | Incremental client (rollout-gated) | Incremental client (rollout-gated) | Distribute the updated APK, then enable both clients together |
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
