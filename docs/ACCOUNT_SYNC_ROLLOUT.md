# Account sync releases

Android and web use incremental entity sync. The legacy `learning_snapshots`
table and `sync_learning_snapshot` RPC are retired and intentionally inaccessible
to signed-in clients. Do not restore their grants to work around a client error;
update the client instead.

Android entity sync is unconditional as of version 0.1.11 (version code 12).
There is no Android `entitySyncEnabled` build flag. The web production
configuration enables entity sync in `webApp/src/wasmJsMain/resources/supabase-config.js`.

## Updating an Android test installation

1. Build an APK with a higher `versionCode` and the same signing certificate as
   the installed app. The GitHub test releases use the local debug certificate.
2. Install it over the existing app. Do not uninstall or clear app data: local
   schedules, tasks, and completion history are stored on the device.
3. Confirm the local data is visible, sign in, and tap **Sync now**.
4. Sign in to the same account on the web app and sync there. Check a few
   schedules and completed tasks before editing on both devices.

The app keeps local data when a sync request fails. Investigate the displayed
error before retrying; the web and Android clients must both use the current
entity API.

Both clients bind their offline entity store to the first authenticated account
that synchronizes it. Signing out keeps device data, but a different account is
not allowed to merge into that store; clear the app/site data only when the user
intentionally wants to discard the existing offline copy and link another
account.

The web outbox acknowledges mutations by mutation ID, not only by entity ID. If
the same item changes while an upload is in flight, the replacement mutation is
retained and rebased to the server revision. Remote pulls likewise rebase but do
not overwrite pending local edits or deletes. These concurrency, duplicate,
retry, delete, and account-binding policies run in CI through
`scripts/entity-sync-policy.test.cjs`. The same gate verifies that fresh access
tokens are reused while expired, malformed, and near-expiry sessions enter the
refresh path before synchronization.

## Release build

For a signed release, configure the signing properties described in
`docs/RELEASE.md` and run:

```powershell
.\scripts\build-release.ps1 -VerifyReproducible
```

For an installation-compatible test APK signed by the local Android debug key:

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug
```
