# Account sync rollout

Incremental account sync must be rolled out in this order. The web gate in
`webApp/src/wasmJsMain/resources/supabase-config.js` stays off until the Android
device checks below are complete.

## Phase 1 — preserve the legacy snapshot

1. On every Android installation that is signed in, open the currently installed
   app and run **Sync now** while it still uses legacy snapshot sync.
2. Resolve any displayed snapshot conflict deliberately. Export a local backup
   from the Android app and the web app before proceeding.
3. Stop using older Android installations after this point. They cannot read or
   write the incremental entity store and would diverge from upgraded clients.

## Phase 2 — install and seed Android

1. Install version 0.1.2 (version code 3) over the existing app. Do not uninstall
   or clear app data; either action would remove the device's offline data.
2. Confirm the package installs as an update. A signature mismatch means the APK
   was not signed with the same key as the installed build; stop rather than
   uninstalling the existing app.
3. Open the upgraded app, confirm the schedules and completion history are intact,
   and run **Sync now**. This first incremental sync seeds the empty entity store.
4. Make one harmless, reversible change (for example, complete then undo a task),
   sync again, and confirm the app reports success.

## Phase 3 — activate and verify web

1. Only after every Android installation is on version code 3 or newer, set the
   web `entitySyncEnabled` gate to `true`, build, and publish the private Site.
2. Open the web app, sign in to the same account, and run **Sync now**. Confirm the
   schedules, task history, and preferences match Android before editing anything.
3. Change one reversible item on Android, sync both clients, and confirm it reaches
   web. Then change it on web, sync both clients, and confirm it reaches Android.
4. Keep the legacy snapshot table and RPC in place during the rollback window.
   If verification fails, turn the web gate off and stop incremental edits while
   investigating; do not ask an old Android build to reconcile the entity store.

## Release build

The final APK must be signed with the existing release key:

```powershell
.\scripts\build-release.ps1 -EntitySyncEnabled -VerifyReproducible
```

For an installation-compatible test build signed by the local Android debug key,
build with `-PentitySyncEnabled=true`. Only use it to update installations that
were originally installed with that same debug key.
