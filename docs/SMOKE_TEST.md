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

On September 24, 2026, the complete suite passed for `0.1.15` (code 16) on the connected Samsung `SM_S916U` emulator. It covered every Room migration through schema 8, fresh-install onboarding, schedule creation and Today retrieval, completion and undo, and a transactional backup round trip. After the production version bump, two clean `1.0.0` (code 17) unsigned release builds passed unit tests and release lint and produced the identical SHA-256 checksum `4EF20A7A8F17F399F68E9B7F852DCFADE94791306CC7D81BB729AE4E95E8F4B8`. A new code-17 device pass remains required because the emulator disconnected before installation.

The reproducibility artifact used `-AllowUnsigned` because no permanent release key is configured. It is verification-only and must not be distributed as a final release.
