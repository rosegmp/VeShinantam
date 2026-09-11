# VeShinantam Web

Compose Multiplatform web MVP for modern browsers. It is a separate Kotlin/Wasm module, depends on the platform-neutral `shared` module, stores app state only in browser local storage, and ships an offline service worker and installable web app manifest.

```powershell
.\gradlew.bat :shared:jvmTest :webApp:wasmJsBrowserDistribution
```

The production site is written to `webApp/build/dist/wasmJs/productionExecutable`.
