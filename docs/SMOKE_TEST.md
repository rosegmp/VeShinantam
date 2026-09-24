# End-to-end smoke test

The device smoke suite exercises the smallest complete offline user-data path without external services or optional AndroidX test-runner downloads.

## Run

Boot an emulator or connect an unlocked Android device, then run:

```powershell
.\gradlew.bat testDebugUnitTest
.\scripts\run-device-smoke-tests.ps1
```

`run-migration-tests.ps1` remains as a compatibility alias for the device smoke script.

## Verified path

1. Build and install the app and device-test APKs.
2. Clear app state and launch `MainActivity` as a first installation.
3. Confirm the localized onboarding is visible in the Android accessibility hierarchy.
4. Open every exported legacy Room schema and migrate each through the production migration chain to schema 8.
5. Seed a version-1 schedule and task and verify the rows and all later defaults survive migration.
6. Create a custom Gemara schedule through `ScheduleRepository` and persist it transactionally.
7. Read its current learning through the production Today query.
8. Complete, undo, and complete the task again, verifying instant, local date, and time-zone history.
9. Serialize schedules, tasks, exclusions, goals, and settings through the production backup format.
10. Clear Room, restore the decoded payload transactionally, and compare every restored record.

The script returns a failure if any build, installation, launch, hierarchy, migration, repository, completion, backup, or restore assertion fails.

## Latest verification

On September 24, 2026, the complete suite passed for `1.0.0` (code 17) on the connected Samsung `SM_S916U` emulator. It covered every Room migration through schema 8, fresh-install onboarding, schedule creation and Today retrieval, completion and undo, and a transactional backup round trip. The production-signed APK then installed cleanly, cold-launched to onboarding, and reported version `1.0.0` / code `17`. Two clean signed builds passed unit tests and release lint and had identical protected APK content; their whole-file signatures differed only because Android's RSA-PSS v2 signature uses random salt. The distributable APK SHA-256 is `AC3BCFA33CF45DEA9BA72B5ADEE0FF6339E47F95EF026C8AD83D5E8BC1ADFAB5`.

The production APK is signed by certificate SHA-256 `D0DB60030F9F6328D00ECB84BDC4D35E3A12AF583F8C7F050D7617BC44B9E8CF`. Preserve the release keystore and its encrypted backup for every future update.
