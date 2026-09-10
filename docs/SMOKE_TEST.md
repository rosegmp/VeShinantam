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
4. Open exported Room schemas 1 through 6 and migrate each through the production migration chain to schema 7.
5. Seed a version-1 schedule and task and verify the rows and all later defaults survive migration.
6. Create a custom Gemara schedule through `ScheduleRepository` and persist it transactionally.
7. Read its current learning through the production Today query.
8. Complete, undo, and complete the task again, verifying instant, local date, and time-zone history.
9. Serialize schedules, tasks, exclusions, goals, and settings through the production backup format.
10. Clear Room, restore the decoded payload transactionally, and compare every restored record.

The script returns a failure if any build, installation, launch, hierarchy, migration, repository, completion, backup, or restore assertion fails.

## Latest verification

On September 10, 2026, the complete suite passed on the `Pixel_10_Pro` emulator running Android 17 / API 37. Two clean `0.1.0` unsigned release builds also produced the identical SHA-256 checksum `0BACF14A77E470BEC5C424EB202C6248BE6563DF038866107AB6B3EFDF49253B`.

The reproducibility artifact used `-AllowUnsigned -SkipLint` because no private release key is configured and this host cannot download the uncached lint runner through its TLS configuration. It is verification-only and must not be distributed as a final release.
