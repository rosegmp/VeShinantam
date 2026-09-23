# Signed preset catalog updates

Android 0.1.13 and the web app can read the same signed catalog at
`https://rosegmp.github.io/VeShinantam/preset-catalog.json`. The bundled
catalog remains available offline. An update changes the presets offered when
creating a schedule; it does not replace a learner's saved schedules or tasks.

## Publish a future update

1. Edit `catalog/preset-catalog.payload.json`. Increase `sequence` above the
   latest published sequence, set `catalogVersion` and `positionAsOf`, and add
   the desired `programs` patches and `positions`.
2. Run `node scripts/preset-catalog.mjs sign`. This signs the exact UTF-8 payload
   bytes using the private key at `artifacts/preset-catalog-private-key.pem` and
   writes `webApp/src/wasmJsMain/resources/preset-catalog.json`.
3. Commit the payload and signed envelope, then push `main`. The GitHub Pages
   workflow publishes the new file. The service worker always requests this
   endpoint from the network, so an old web cache cannot hide an update.
4. On Android, open Settings and tap **Check for updates**, or enable automatic
   updates for daily checks. On web, enable **Automatic catalog updates** in
   Settings; the app reloads and checks the signed file.

The private key is ignored by Git and must be backed up securely outside this
workspace. Never commit it or upload it to GitHub. Losing it requires a new APK
with a new pinned public key. `node scripts/preset-catalog.mjs init-key` creates
the key only if one does not already exist; it must not be run for every update.

## Version 2 payload

`programs` is a list of changes. For an existing ID, include only fields to
replace. For a new ID, provide `nameEnglish`, `nameHebrew`, `materialType`,
`dailyQuantity`, `selectedWeekdays`, and at least one bilingual `units` entry.
`positions` maps any changed or new preset ID to the exact English text of its
starting unit. Unchanged bundled positions advance from the bundled anchor
date to `positionAsOf` automatically.

```json
{
  "schemaVersion": 2,
  "catalogVersion": "2026.10.01-12",
  "sequence": 12,
  "positionAsOf": "2026-10-01",
  "positions": { "new-daily-program": "First section" },
  "programs": [
    {
      "id": "new-daily-program",
      "nameEnglish": "New Daily Program",
      "nameHebrew": "תוכנית יומית חדשה",
      "materialType": "CUSTOM_UNIT",
      "dailyQuantity": 1,
      "selectedWeekdays": [0, 1, 2, 3, 4, 5],
      "excludedDates": [],
      "units": [
        { "english": "First section", "hebrew": "קטע ראשון" },
        { "english": "Second section", "hebrew": "קטע שני" }
      ]
    }
  ]
}
```

Weekdays use Sunday `0` through Saturday `6`. Material types are `DAF`,
`AMUD`, `MISHNAH`, `PEREK`, `PAGE`, `SEIF`, `SIMAN`, and `CUSTOM_UNIT`.
An existing program can change its names, pace, weekdays, exclusions, or units;
if its units change, set a `positions` entry that exists in the new unit list.
To keep a previously added remote program in a later catalog, include its full
definition again: every signed catalog is evaluated against the bundled catalog.
The current signed envelope and decoded payload must each fit within 1 MiB.

The signature uses ECDSA P-256 with SHA-256 over the decoded payload bytes.
Android and web pin the same DER SubjectPublicKeyInfo public key and reject
invalid signatures, unknown material types, duplicate IDs or units, invalid
dates, and unknown starting references. The parser still accepts version 1
position-only payloads signed by the current key. Cached files signed by an
older key are discarded and the bundled catalog is used until a new check.

Catalog updates contain schedule references and rules. The bundled Pele Yoetz
full text and in-app reader code are separate APK/web assets and are not
replaced by this catalog format.
