# Release signing and private distribution

VeShinantam uses Android's standard APK signing. Debug APKs are suitable for local testing only. Every privately distributed release must be signed with the same protected release key so Android can install future versions as updates without removing the app and its local data.

## 1. Create and protect the release key

Run this once from a trusted machine with JDK 17 or newer:

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\VeShinantam-release.jks" -alias veshinantam -keyalg RSA -keysize 4096 -validity 10000
```

Keep the keystore and its passwords outside this project. Store an encrypted backup in a second secure location. Losing the key prevents future APKs from updating existing installations; exposing it allows someone else to sign a malicious update.

## 2. Configure signing locally

Add all four properties to `%USERPROFILE%\.gradle\gradle.properties`. Do not put passwords in the project or commit them to source control.

```properties
releaseStoreFile=C:/Users/YOUR_NAME/VeShinantam-release.jks
releaseStorePassword=YOUR_STORE_PASSWORD
releaseKeyAlias=veshinantam
releaseKeyPassword=YOUR_KEY_PASSWORD
```

The build fails early if only some signing properties are supplied. With none supplied, debug builds continue to work and the release build remains unsigned.

## 3. Version and build

Before distributing an update, increase `versionCode` in `app/build.gradle.kts`. Also update the user-facing `versionName`.

```powershell
.\gradlew.bat clean testDebugUnitTest
.\scripts\run-device-smoke-tests.ps1
.\scripts\build-release.ps1 -VerifyReproducible
```

The release script writes the versioned APK and its `.sha256` file to `artifacts/`. With `-VerifyReproducible`, it builds twice from a clean tree. Unsigned APKs must match byte for byte. Android's preferred RSA-PSS v2 signatures contain random salt, so signed APKs may have different whole-file hashes; in that case the script requires all protected APK content to match byte for byte, verifies both signatures, and requires the same signer certificate. The `.sha256` file always records the exact distributable APK bytes.

The device smoke script requires one booted emulator or connected device. It checks fresh-install launch and onboarding, every Room migration path, schedule creation, Today retrieval, completion and undo, and a transactional backup round trip. It avoids Gradle's optional Unified Test Platform download and can therefore run after the project's Android dependencies have been cached for offline use.

For diagnostics only, `build-release.ps1 -AllowUnsigned -SkipLint` permits an unsigned package while explicitly warning that it must not be distributed. A final release must be signed and must run without `-SkipLint`.

## 4. Verify before sharing

Use the `apksigner` from the installed Android SDK Build Tools:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```

Record the version name, version code, SHA-256 checksum, build date, and certificate SHA-256 digest with every distributed APK. Compare the checksum after copying or downloading the file.

## 5. Share privately

Place the verified APK in a private OneDrive folder or another access-controlled location and share it only with intended testers. Send the checksum through a separate trusted channel when practical. Android may ask the recipient to allow installs from the browser or file-manager app used to open the APK.

Do not publish the keystore, Gradle password file, or preset-catalog signing private key with the APK. Back up current app data before installing a release candidate, and never reduce `versionCode` for an update.

The GitHub test APKs are debug-signed. Android will not install a production-signed APK over them. Before moving a test installation to production, export a backup, uninstall the test APK, install the production APK, and restore the backup. Future production releases will update in place as long as they use the same release key.

## Release checklist

- Unit tests and database migration tests pass.
- Backup compatibility fixtures for every supported backup version pass.
- Local and deployed Supabase migration versions match; security and performance advisors have no unreviewed findings.
- Live Android-to-web sync passes with one production account, including a completion made on each client and a sign-out/sign-in token refresh.
- English/Hebrew light, dark, RTL, and large-font smoke checks pass.
- TalkBack passes on a physical phone, and the supported minimum and maximum Android API levels pass smoke testing.
- The browser matrix in `WEB_BROWSER_QA.md` is recorded for the release candidate.
- `versionCode` is greater than the previously distributed build.
- APK signature and certificate digest are verified.
- APK SHA-256 is recorded and matches the distributed copy.
- A restore-tested backup and an encrypted copy of the release keystore exist.
