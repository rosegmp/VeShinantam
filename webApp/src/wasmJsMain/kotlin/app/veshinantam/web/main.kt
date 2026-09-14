package app.veshinantam.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalWasmJsInterop

private const val StorageKey = "veshinantam.web.v1"
private const val PendingImportKey = "veshinantam.web.pending-import"
private const val InvalidImportMarker = "__VESHINANTAM_INVALID_BACKUP__"

private class LocalBrowserStore(private val today: String) : BrowserStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val backupJson = Json { prettyPrint = true }

    override fun load(): WebAppState {
        val raw = readLocalStorage(StorageKey) ?: return WebAppState.sample(today).also(::save)
        return runCatching { json.decodeFromString<WebAppState>(raw) }.getOrElse { WebAppState.sample(today) }
    }

    override fun save(state: WebAppState) {
        writeBrowserState(json.encodeToString(state))
    }

    override fun currentInstant(): String = currentIsoInstant()
    override fun currentZoneId(): String = browserTimeZone()

    override fun exportBackup(state: WebAppState) {
        val now = currentIsoInstant()
        downloadTextFile(
            "veshinantam-backup-$today.json",
            backupJson.encodeToString(WebBackup(state = state, canonical = state.toCanonical(now, today))),
        )
    }

    override fun requestBackupImport() {
        chooseBackupFile(PendingImportKey, InvalidImportMarker)
    }

    override fun consumeBackupImport(): BackupImportResult {
        val raw = readSessionStorage(PendingImportKey) ?: return BackupImportResult.None
        removeSessionStorage(PendingImportKey)
        if (raw == InvalidImportMarker) return BackupImportResult.Invalid
        val state = runCatching { json.decodeFromString<WebBackup>(raw).validStateOrNull() }.getOrNull()
        return if (state == null) BackupImportResult.Invalid else BackupImportResult.Ready(state)
    }
}

private class SupabaseCloudAccount : CloudAccount {
    private val json = Json { ignoreUnknownKeys = true }

    override fun state(): CloudAccountState {
        val raw = cloudAccountState()
        return runCatching { json.decodeFromString<CloudAccountState>(raw) }
            .getOrElse { CloudAccountState(configured = false, status = "Account status could not be read.") }
    }

    override fun requestMagicLink(email: String) = sendMagicLink(email)
    override fun sync(state: WebAppState) = startCloudSync(json.encodeToString(state))
    override fun useCloudCopy() = resolveCloudSync("cloud", "")
    override fun replaceCloudCopy(state: WebAppState) = resolveCloudSync("device", json.encodeToString(state))
    override fun signOut() = cloudSignOut()
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.localStorage.getItem(key)")
private external fun readLocalStorage(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(value) => window.veshinantamPersistState(value)")
private external fun writeBrowserState(value: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.getItem(key)")
private external fun readSessionStorage(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.removeItem(key)")
private external fun removeSessionStorage(key: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(filename, text) => {
    const blob = new Blob([text], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
}""")
private external fun downloadTextFile(filename: String, text: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(key, invalidMarker) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'application/json,.json';
    input.onchange = () => {
        const file = input.files && input.files[0];
        if (!file) return;
        if (file.size > 2 * 1024 * 1024) {
            sessionStorage.setItem(key, invalidMarker);
            location.reload();
            return;
        }
        const reader = new FileReader();
        reader.onload = () => {
            sessionStorage.setItem(key, typeof reader.result === 'string' ? reader.result : invalidMarker);
            location.reload();
        };
        reader.onerror = () => {
            sessionStorage.setItem(key, invalidMarker);
            location.reload();
        };
        reader.readAsText(file);
    };
    input.click();
}""")
private external fun chooseBackupFile(key: String, invalidMarker: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.veshinantamAccountState()")
private external fun cloudAccountState(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(email) => window.veshinantamSendMagicLink(email)")
private external fun sendMagicLink(email: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(state) => window.veshinantamSync(state)")
private external fun startCloudSync(state: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(choice, state) => window.veshinantamResolveSync(choice, state)")
private external fun resolveCloudSync(choice: String, state: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.veshinantamSignOut()")
private external fun cloudSignOut()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => new Date().toISOString().slice(0, 10)")
private external fun currentIsoDate(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => new Date().toISOString()")
private external fun currentIsoInstant(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'")
private external fun browserTimeZone(): String

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val today = currentIsoDate()
    ComposeViewport(document.body!!) {
        WebApp(LocalBrowserStore(today), SupabaseCloudAccount(), today)
    }
}
