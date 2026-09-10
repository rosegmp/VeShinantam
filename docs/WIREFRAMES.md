# VeShinantam — screen-by-screen wireframes

These are behavior wireframes, not fixed-pixel layouts. In Hebrew, the hierarchy mirrors to RTL; bilingual Torah references remain readable as a paired block.

## A. First launch / welcome

```text
┌──────────────────────────────────┐
│          [sefer ↻ mark]          │
│          VeShinantam             │
│                                  │
│  Your learning and chazarah,     │
│  planned privately on this phone │
│                                  │
│  • Works offline                 │
│  • English and עברית             │
│  • Your history stays yours      │
│                                  │
│                 [Get started]    │
└──────────────────────────────────┘
```

Primary action advances into first-schedule creation. Language can be selected from a compact English / עברית control; changing it immediately mirrors the flow.

## B. Schedule wizard — choose program

```text
┌──────────────────────────────────┐
│ ←  Create your first schedule 1/6│
│ What are you learning?           │
│                                  │
│ Presets                          │
│ ( ) Daf Yomi Bavli / דף יומי     │
│ ( ) Oraysa / אורייתא             │
│ ( ) Amud Yomi / עמוד יומי        │
│ ( ) Mishnah Yomis / משנה יומית   │
│ ( ) Dirshu Mishnah Berurah / ... │
│                                  │
│ Your own plan                    │
│ ( ) Structured material          │
│ ( ) Free-form units              │
│                         [Next]   │
└──────────────────────────────────┘
```

## C. Schedule wizard — position/material

```text
┌──────────────────────────────────┐
│ ←  Starting position          2/6│
│ [Join current worldwide position]│
│                                  │
│ Or choose an earlier position    │
│ Masechta / מסכת   [Berachos   ▾] │
│ Daf / דף           [  12       ] │
│ Side / עמוד        [ a / א     ▾]│
│                                  │
│ Preview: Berachos 12a / ברכות יב.│
│                         [Next]   │
└──────────────────────────────────┘
```

Custom structured plans first select Gemara, Mishnah, Mishnah Berurah, Kitzur, or Other. Gemara and Mishnah then choose a From masechta and a To masechta, followed by start/end dropdowns populated with valid locations. Gemara selects daf or amud; Mishnah selects individual mishnayos or whole perakim. Mishnah Berurah selects a chelek and page/seif/siman before choosing from unit-specific start/end lists. Kitzur selects from pre-populated siman lists. A custom schedule has one name field, and Other adds units one at a time through one label field. Both fields accept either English or Hebrew and preserve the entered text; the user is never asked to enter both. No multiline import field appears.

## D. Schedule wizard — pace and dates

```text
┌──────────────────────────────────┐
│ ←  Pace and dates             3/6│
│ Start date       [Sep 8, 2026]   │
│                                  │
│ Plan by                          │
│ (•) Daily pace   ( ) Finish by   │
│ Quantity         [ 1 ] [daf ▾]   │
│                                  │
│ Learning days                   │
│ [S] [M] [T] [W] [T] [F] [S]    │
│                                  │
│ If learning is missed           │
│ (•) Shift unfinished work       │
│ ( ) Keep date and show overdue  │
│                         [Next]   │
└──────────────────────────────────┘
```

Finish-by mode replaces quantity with target date; the preview explains the computed varying daily assignment without warning language.

## E. Schedule wizard — chazarah

```text
┌──────────────────────────────────┐
│ ←  Chazarah                   4/6│
│ (•) Use default pattern          │
│     1, 7, 30, 180 days; yearly  │
│ ( ) Customize for this schedule  │
│                                  │
│ [1] [7] [30] [180] days         │
│ [✓] Continue every year          │
│                                  │
│ Reviews only land on selected    │
│ learning days.                   │
│                         [Next]   │
└──────────────────────────────────┘
```

## F. Schedule wizard — reminder

```text
┌──────────────────────────────────┐
│ ←  Daily reminder             5/6│
│ [✓] Remind me once each day      │
│ Time                   [8:00 PM] │
│                                  │
│ One reminder summarizes all due  │
│ learning and chazarah.            │
│                         [Next]   │
└──────────────────────────────────┘
```

Notification permission is requested only after opt-in and at the platform-appropriate moment.

## G. Schedule wizard — preview and save

```text
┌──────────────────────────────────┐
│ ←  Review your plan           6/6│
│ Daf Yomi Bavli / דף יומי         │
│ Sep 8, 2026 → Jan 14, 2034       │
│ 1 daf • Sun–Thu, Sat             │
│                                  │
│ First 7 learning days            │
│ Tue  Berachos 12 / ברכות יב      │
│ Wed  Berachos 13 / ברכות יג      │
│ ...                              │
│                                  │
│ Upcoming chazarah                │
│ 7 days: 12 tasks • 30 days: 54   │
│                                  │
│                  [Save schedule] │
└──────────────────────────────────┘
```

Preview always shows daily workload, computed completion date, and future review volume.

## H. Today (default landing)

```text
┌──────────────────────────────────┐
│ VeShinantam                 [⋮]  │
│ Monday • 25 Elul 5786             │
│                                  │
│ Daf Yomi Bavli / דף יומי     2/4 │
│ NEW LEARNING                     │
│ [ ] Berachos 12 / ברכות יב       │
│ CHAZARAH DUE TODAY               │
│ [✓] Berachos 11 / ברכות יא       │
│ OVERDUE LEARNING                 │
│ [ ] Berachos 10 / ברכות י  Sep 5 │
│ OVERDUE CHAZARAH                 │
│ [ ] Berachos 8 / ברכות ח   Sep 3 │
│                                  │
│ Today  Calendar  Schedules  Stats│
└──────────────────────────────────┘
```

Each row toggles only itself. Completed rows stay visible for the day. Empty state celebrates completion gently and shows the next scheduled date.

## I. Calendar

```text
┌──────────────────────────────────┐
│ Calendar                    [⋮]  │
│ [All schedules ▾] [All types ▾]  │
│           ‹ September 2026 ›     │
│ S   M   T   W   T   F   S        │
│         1   2   3   4   5        │
│        כ״א כ״ב כ״ג כ״ד כ״ה      │
│ ...  dates carry ○ ◐ ● status ...│
│                                  │
│ Mon, Sep 7 • 25 Elul 5786        │
│ [ ] Learning: ...                │
│ [✓] Chazarah: ...                │
│                                  │
│ Today  Calendar  Schedules  Stats│
└──────────────────────────────────┘
```

Status is expressed by icon/shape plus accessible text, not color alone. Selecting any future date permits early individual completion.

## J. Schedules list

```text
┌──────────────────────────────────┐
│ Schedules                   [⋮]  │
│                                  │
│ ┌ Daf Yomi Bavli / דף יומי ───┐ │
│ │ Active • 63% • next: יב      │ │
│ │ Sun–Thu, Sat • 1 daf         │ │
│ │                   [Manage]   │ │
│ └──────────────────────────────┘ │
│ ┌ Mishnayos / משניות ─────────┐ │
│ │ Paused                       │ │
│ └──────────────────────────────┘ │
│                  [+ New schedule]│
│ Today  Calendar  Schedules  Stats│
└──────────────────────────────────┘
```

Archived schedules live behind an “Archived” filter and never mix with current due work.

## K. Schedule detail/manage

```text
┌──────────────────────────────────┐
│ ← Daf Yomi Bavli / דף יומי  [⋮] │
│ Active • 1 daf • selected days   │
│                                  │
│ Progress              1,024/2,711│
│ Next learning         Berachos 12│
│ Next 30d reviews              118│
│                                  │
│ [Edit future plan] [Pause]       │
│ [Excluded dates] [Chazarah]      │
│ [Archive]                        │
└──────────────────────────────────┘
```

Delete is in the overflow menu and requires explicit destructive confirmation. Edit preview states how many unfinished future tasks will be replaced and that completed history is preserved.

## L. Progress

```text
┌──────────────────────────────────┐
│ Progress                    [⋮]  │
│ Current streak       18 scheduled│
│ Overall completion            74%│
│ [gold milestone: 1,000 units]    │
│                                  │
│ Totals                           │
│ Dapim 384  Amudim 96  Mishnayos… │
│                                  │
│ Upcoming chazarah                │
│ 7d ▂▅▃▇▂▃▅  30d total: 118       │
│                                  │
│ Goals                     [Add]  │
│ Today  Calendar  Schedules  Stats│
└──────────────────────────────────┘
```

The streak explanation explicitly says that rest, pause, and excluded days are skipped.

## M. Settings

```text
┌──────────────────────────────────┐
│ ← Settings                       │
│ Language       [English / עברית] │
│                                  │
│ Default chazarah                  │
│ 1, 7, 30, 180 days • yearly  [>]│
│                                  │
│ Daily reminder         [on]      │
│ Reminder time          8:00 PM   │
│                                  │
│ Preset schedules                 │
│ Bundled version 1       [Update] │
└──────────────────────────────────┘
```

There is no theme row; system theme is automatic.

## N. Backup, restore, and printable PDF

```text
┌──────────────────────────────────┐
│ ← Data                            │
│ Backup & restore                  │
│ [Export backup file]              │
│ [Import backup file]              │
│ Last backup: Sep 6, 2026          │
│                                   │
│ Printable schedule                │
│ Number of upcoming days [ 30 ]    │
│ [Preview PDF] [Save PDF]          │
└───────────────────────────────────┘
```

Import first validates and summarizes the file, then asks whether to replace local data. The PDF preview shows dates, bilingual references, task type, and an empty checkbox for every obligation.

## O. Home-screen widget

```text
┌──────────────────────────┐
│ VeShinantam       7 due  │
│ □ Berachos 12 / ברכות יב │
│ □ Review: Berachos 11    │
│ + 5 more                 │
└──────────────────────────┘
```

The widget is informational; tapping a row or body opens Today, where completion remains individual and fully accessible.
