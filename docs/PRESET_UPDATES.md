# Signed preset catalog updates

Normal app use is fully offline. A release build can opt into preset-position updates by setting the Gradle property `presetCatalogUpdateUrl` to an HTTPS URL. When the property is absent, update controls explain that the bundled catalog is in use and no network request is made.

## Trust model

- The app accepts only HTTPS and does not follow redirects.
- Downloads and decoded payloads are limited to 1 MiB.
- The outer JSON envelope contains `format`, `keyId`, base64 `payload`, and a base64 DER ECDSA signature.
- The signature is SHA-256 with ECDSA over the exact decoded payload bytes, using a pinned P-256 public key.
- The payload contains schema version `1`, a display version, a strictly increasing sequence number, an as-of date, and one current English reference for every bundled preset ID.
- Every ID and reference must resolve against the bundled reference-only material. Updates cannot add sefer text, modify schedules, or read/upload progress.
- Verified files are flushed and atomically renamed into the private app files directory. Invalid cached files are discarded and the bundled catalog remains available.

Example envelope:

```json
{
  "format": "app.veshinantam.preset-catalog",
  "keyId": "veshinantam-preset-v1",
  "payload": "BASE64_OF_EXACT_PAYLOAD_BYTES",
  "signature": "BASE64_OF_DER_ECDSA_SIGNATURE"
}
```

Example decoded payload:

```json
{
  "schemaVersion": 1,
  "catalogVersion": "2026.09.10-8",
  "sequence": 8,
  "positionAsOf": "2026-09-09",
  "positions": {
    "daf-yomi-bavli": "Chullin 132",
    "oraysa": "Yevamos 104b"
  }
}
```

The real payload must include every bundled preset ID. Before private distribution, generate and securely retain an offline P-256 signing key, provide its base64 DER SubjectPublicKeyInfo value as `presetCatalogPublicKey`, and never commit the private key. Configure builds with, for example:

```powershell
.\gradlew.bat -PpresetCatalogUpdateUrl=https://updates.example.org/veshinantam/catalog.json -PpresetCatalogPublicKey=BASE64_DER_PUBLIC_KEY assembleRelease
```

Signature rotation is intentionally deferred to the later diagnostics/key-rotation enhancement; the current schema recognizes one explicit key ID.

## Web configuration

The web app accepts the same envelope, key ID, DER ECDSA signature, and payload as Android. Configure `window.VESHINANTAM_PRESET_UPDATES` in `supabase-config.js` with the HTTPS endpoint and the same base64 DER SubjectPublicKeyInfo value. The browser verifies the signature with WebCrypto, while the Kotlin layer independently requires a newer sequence and one resolvable reference for every bundled preset.

Verified envelopes are cached in browser storage. The cache is reverified at every startup; invalid, incomplete, or unknown-reference catalogs are discarded, and the bundled catalog remains active. Enabling automatic catalog updates reloads the app once so it can perform the verified check before schedule creation begins.
