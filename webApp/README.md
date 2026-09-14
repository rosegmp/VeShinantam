# VeShinantam Web

Compose Multiplatform web MVP for modern browsers. It is a separate Kotlin/Wasm module, depends on the platform-neutral `shared` module, stores app state in IndexedDB with a synchronous local-storage startup mirror, and ships an offline service worker and installable web app manifest. Existing local-storage data migrates automatically on first load. An IndexedDB mutation outbox supports incremental entity synchronization when the matching Supabase schema is available and safely falls back to the legacy snapshot protocol during rollout.

```powershell
.\gradlew.bat :shared:jvmTest :webApp:wasmJsBrowserDistribution
```

The production site is written to `webApp/build/dist/wasmJs/productionExecutable`.
