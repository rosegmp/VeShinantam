package app.veshinantam.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.runtime.remember
import app.veshinantam.shared.CanonicalDataCodec
import kotlinx.browser.document
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalWasmJsInterop

private const val PendingImportKey = "veshinantam.web.pending-import"
private const val InvalidImportMarker = "__VESHINANTAM_INVALID_BACKUP__"

private class LocalBrowserStore : BrowserStore {
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): WebAppState {
        val raw = readBrowserState() ?: return WebAppState.sample(currentIsoDate()).also(::save)
        return runCatching { json.decodeFromString<WebAppState>(raw) }.getOrElse { WebAppState.sample(currentIsoDate()) }
    }

    override fun save(state: WebAppState) {
        writeBrowserState(json.encodeToString(state))
    }

    override fun currentLocalDate(): String = currentIsoDate()
    override fun hebrewDateLabel(date: String, hebrewUi: Boolean): String = formatHebrewDate(date, hebrewUi)
    override fun hebrewDayLabel(date: String, hebrewUi: Boolean): String = formatHebrewDay(date, hebrewUi)
    override fun hebrewCalendarPeriod(date: String, hebrewUi: Boolean): WebCalendarPeriod =
        runCatching { json.decodeFromString<WebCalendarPeriod>(hebrewCalendarPeriodJson(date, hebrewUi)) }
            .getOrElse { WebCalendarPeriod(date, date, formatHebrewDate(date, hebrewUi)) }
    override fun currentInstant(): String = currentIsoInstant()
    override fun currentZoneId(): String = browserTimeZone()

    override fun exportBackup(state: WebAppState) {
        val today = currentIsoDate()
        val now = currentIsoInstant()
        downloadTextFile(
            "veshinantam-backup-$today.json",
            CanonicalDataCodec.encode(state.toCanonical(now, today)),
        )
    }

    override fun requestBackupImport() {
        chooseBackupFile(PendingImportKey, InvalidImportMarker)
    }

    override fun consumeBackupImport(): BackupImportResult {
        val raw = readSessionStorage(PendingImportKey) ?: return BackupImportResult.None
        removeSessionStorage(PendingImportKey)
        if (raw == InvalidImportMarker) return BackupImportResult.Invalid
        val state = decodeImportedBackup(raw, currentIsoInstant(), currentIsoDate())
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

    override fun signIn(email: String, password: String) = passwordSignIn(email, password)
    override fun createAccount(email: String, password: String) = passwordCreateAccount(email, password)
    override fun sync(state: WebAppState) = startCloudSync(json.encodeToString(state))
    override fun useCloudCopy() = resolveCloudSync("cloud", "")
    override fun replaceCloudCopy(state: WebAppState) = resolveCloudSync("device", json.encodeToString(state))
    override fun signOut() = cloudSignOut()
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.veshinantamReadState()")
private external fun readBrowserState(): String?

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
@JsFun("(email, password) => window.veshinantamPasswordSignIn(email, password)")
private external fun passwordSignIn(email: String, password: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(email, password) => window.veshinantamPasswordCreateAccount(email, password)")
private external fun passwordCreateAccount(email: String, password: String)

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
@JsFun("() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); }")
private external fun currentIsoDate(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => new Date().toISOString()")
private external fun currentIsoInstant(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'")
private external fun browserTimeZone(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(iso, hebrewUi) => {
    const value = new Date(iso + 'T12:00:00Z');
    if (Number.isNaN(value.getTime())) return iso;
    return new Intl.DateTimeFormat(hebrewUi ? 'he-IL-u-ca-hebrew' : 'en-US-u-ca-hebrew', {
        day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC'
    }).format(value);
}""")
private external fun formatHebrewDate(iso: String, hebrewUi: Boolean): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(iso, hebrewUi) => {
    const value = new Date(iso + 'T12:00:00Z');
    if (Number.isNaN(value.getTime())) return '';
    return new Intl.DateTimeFormat(hebrewUi ? 'he-IL-u-ca-hebrew' : 'en-US-u-ca-hebrew', {
        day: 'numeric', timeZone: 'UTC'
    }).format(value);
}""")
private external fun formatHebrewDay(iso: String, hebrewUi: Boolean): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(iso, hebrewUi) => {
    const atNoon = value => new Date(value + 'T12:00:00Z');
    const toIso = value => value.toISOString().slice(0, 10);
    const addDays = (value, days) => {
        const result = atNoon(value);
        result.setUTCDate(result.getUTCDate() + days);
        return toIso(result);
    };
    const partsFormatter = new Intl.DateTimeFormat('en-US-u-ca-hebrew', {
        day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC'
    });
    const signature = value => {
        const parts = partsFormatter.formatToParts(atNoon(value));
        const month = parts.find(part => part.type === 'month')?.value || '';
        const year = parts.find(part => part.type === 'year' || part.type === 'relatedYear')?.value || '';
        return month + '|' + year;
    };
    if (Number.isNaN(atNoon(iso).getTime())) return JSON.stringify({ startDate: iso, endDate: iso, title: iso });
    const expected = signature(iso);
    let start = iso;
    let end = iso;
    for (let count = 0; count < 35; count++) {
        const previous = addDays(start, -1);
        if (signature(previous) !== expected) break;
        start = previous;
    }
    for (let count = 0; count < 35; count++) {
        const next = addDays(end, 1);
        if (signature(next) !== expected) break;
        end = next;
    }
    const title = new Intl.DateTimeFormat(hebrewUi ? 'he-IL-u-ca-hebrew' : 'en-US-u-ca-hebrew', {
        month: 'long', year: 'numeric', timeZone: 'UTC'
    }).format(atNoon(start));
    return JSON.stringify({ startDate: start, endDate: end, title });
}""")
private external fun hebrewCalendarPeriodJson(iso: String, hebrewUi: Boolean): String

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        val store = remember { LocalBrowserStore() }
        val cloudAccount = remember { SupabaseCloudAccount() }
        WebApp(store, cloudAccount)
    }
}
