package app.veshinantam.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.runtime.remember
import app.veshinantam.shared.CanonicalDataCodec
import kotlinx.browser.document
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import app.veshinantam.shared.text.SefariaTextRequest
import kotlinx.coroutines.delay
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

    override fun printSchedule(state: WebAppState, dayCount: Int) {
        val printable = buildPrintableSchedule(state, currentIsoDate(), dayCount)
        val hebrew = state.language == "he"
        val title = if (hebrew) "סדר הלימוד של ושיננתם" else "VeShinantam Learning Schedule"
        val range = if (hebrew) {
            "${printDateLabel(printable.startDate, state)} עד ${printDateLabel(printable.endDate, state)}"
        } else {
            "${printDateLabel(printable.startDate, state)} through ${printDateLabel(printable.endDate, state)}"
        }
        val groupedRows = printable.rows.groupBy { it.date }.entries.joinToString("") { (date, rows) ->
            """<section class="day"><h2>${escapeHtml(printDateLabel(date, state))}</h2>${rows.joinToString("") { row ->
                val checkbox = if (row.completed) """<span class="box completed">✓</span>""" else """<span class="box"></span>"""
                """<div class="task">$checkbox<div><strong>${escapeHtml(row.scheduleName)} — ${escapeHtml(row.taskType)}</strong><br><span>${escapeHtml(row.reference)}</span></div></div>"""
            }}</section>"""
        }
        val content = groupedRows.ifEmpty {
            "<p class=\"empty\">${if (hebrew) "אין משימות לימוד או חזרה פעילות בטווח תאריכים זה." else "No active learning or chazarah tasks are scheduled in this date range."}</p>"
        }
        val html = """<!doctype html><html lang="${if (hebrew) "he" else "en"}" dir="${if (hebrew) "rtl" else "ltr"}"><head><meta charset="utf-8"><title>${escapeHtml(title)}</title><style>
            @page { size: A4; margin: 16mm 15mm 18mm; }
            * { box-sizing: border-box; }
            body { margin: 0; color: #23262a; font-family: "Noto Sans Hebrew", Arial, sans-serif; font-size: 10.5pt; }
            header { border-bottom: 2px solid #c9992e; margin-bottom: 14px; padding-bottom: 10px; }
            h1 { color: #153b5b; font-size: 22pt; margin: 0 0 4px; }
            .range { color: #5c6269; font-size: 9.5pt; }
            .day { margin: 0 0 12px; }
            h2 { background: #f9f1db; border-radius: 5px; break-after: avoid; color: #153b5b; font-size: 11.5pt; margin: 0; padding: 6px 9px; }
            .task { align-items: flex-start; border-bottom: 1px solid #dcded0; display: flex; gap: 9px; min-height: 42px; padding: 8px 2px; break-inside: avoid; }
            .box { align-items: center; border: 1.5px solid #153b5b; color: #153b5b; display: inline-flex; flex: 0 0 15px; font-size: 13px; font-weight: bold; height: 15px; justify-content: center; line-height: 1; margin-top: 2px; width: 15px; }
            .empty { color: #5c6269; }
            @media screen { body { margin: 24px auto; max-width: 180mm; padding: 0 8px; } }
        </style></head><body><header><h1>${escapeHtml(title)}</h1><div class="range">${escapeHtml(range)}</div></header>$content<script>window.addEventListener('load',()=>setTimeout(()=>window.print(),100));</script></body></html>"""
        openPrintDocument(html)
    }

    override fun updateDueBadge(state: WebAppState, today: String) {
        setApplicationBadge(dueBadgeCount(state, today))
    }

    override fun requestReminderPermission() {
        requestNotificationPermission()
    }

    override fun updateBrowserReminder(state: WebAppState, today: String) {
        scheduleBrowserReminder(
            enabled = state.reminderEnabled,
            hour = state.reminderHour,
            minute = state.reminderMinute,
            dueCount = dueBadgeCount(state, today),
            hebrew = state.language == "he",
        )
    }

    override fun refreshPresetCatalog() = reloadForPresetCatalog()

    override fun readCachedText(cacheKey: String): String? = readSefariaCache(cacheKey)

    override fun cacheText(cacheKey: String, raw: String) = writeSefariaCache(cacheKey, raw)

    override suspend fun fetchSefariaText(request: SefariaTextRequest): String {
        beginSefariaRequest(request.cacheKey, request.reference, request.versions.joinToString("\n"))
        repeat(150) {
            when (sefariaRequestStatus(request.cacheKey)) {
                "ready" -> return requireNotNull(sefariaRequestBody(request.cacheKey))
                "error" -> error(sefariaRequestError(request.cacheKey) ?: "The text could not be loaded.")
            }
            delay(200)
        }
        error("The text request timed out.")
    }

    private fun printDateLabel(date: String, state: WebAppState): String {
        val gregorian = formatGregorianDate(date, state.language == "he")
        val hebrew = formatHebrewDate(date, state.language == "he")
        return if (state.primaryCalendar == "HEBREW") "$hebrew — $gregorian" else "$gregorian — $hebrew"
    }
}

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

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
@JsFun("() => window.veshinantamReadPresetCatalog()")
private external fun readPresetCatalog(): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.veshinantamDiscardPresetCatalog()")
private external fun discardPresetCatalog()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.location.reload()")
private external fun reloadForPresetCatalog()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.getItem(key)")
private external fun readSessionStorage(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.removeItem(key)")
private external fun removeSessionStorage(key: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.veshinantamReadSefariaCache(key)")
private external fun readSefariaCache(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key, raw) => window.veshinantamWriteSefariaCache(key, raw)")
private external fun writeSefariaCache(key: String, raw: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key, reference, versions) => window.veshinantamBeginSefariaRequest(key, reference, versions)")
private external fun beginSefariaRequest(key: String, reference: String, versions: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.veshinantamSefariaRequestStatus(key)")
private external fun sefariaRequestStatus(key: String): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.veshinantamSefariaRequestBody(key)")
private external fun sefariaRequestBody(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.veshinantamSefariaRequestError(key)")
private external fun sefariaRequestError(key: String): String?

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
@JsFun("""(html) => {
    const popup = window.open('', '_blank');
    if (!popup) { window.print(); return; }
    popup.document.open();
    popup.document.write(html);
    popup.document.close();
    popup.focus();
}""")
private external fun openPrintDocument(html: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(count) => {
    try {
        if (count > 0 && typeof navigator.setAppBadge === 'function') navigator.setAppBadge(count).catch(() => {});
        else if (typeof navigator.clearAppBadge === 'function') navigator.clearAppBadge().catch(() => {});
    } catch (_) { /* Badging is best-effort and unavailable in some browsers. */ }
}""")
private external fun setApplicationBadge(count: Int)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""() => {
    if ('Notification' in window && Notification.permission === 'default') {
        Promise.resolve(Notification.requestPermission()).catch(() => {});
    }
}""")
private external fun requestNotificationPermission()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(enabled, hour, minute, dueCount, hebrew) => {
    if (window.__veshinantamReminderTimer) {
        clearTimeout(window.__veshinantamReminderTimer);
        window.__veshinantamReminderTimer = 0;
    }
    if (!enabled || !('Notification' in window)) return;
    const scheduleNext = () => {
        const now = new Date();
        const next = new Date(now);
        next.setHours(hour, minute, 0, 0);
        if (next <= now) next.setDate(next.getDate() + 1);
        const delay = Math.max(1000, next.getTime() - now.getTime());
        window.__veshinantamReminderTimer = setTimeout(async () => {
            if (Notification.permission === 'granted' && dueCount > 0) {
                const title = hebrew ? 'הגיע זמן הלימוד' : 'Time for today’s learning';
                const body = hebrew
                    ? dueCount + ' משימות לימוד וחזרה ממתינות לך.'
                    : dueCount + ' learning and chazarah ' + (dueCount === 1 ? 'task is' : 'tasks are') + ' waiting.';
                const options = { body, icon: './icon.svg', badge: './icon.svg', tag: 'veshinantam-daily-reminder', renotify: true, data: { url: './?view=today' } };
                try {
                    if ('serviceWorker' in navigator) {
                        const registration = await navigator.serviceWorker.ready;
                        await registration.showNotification(title, options);
                    } else {
                        new Notification(title, options);
                    }
                } catch (_) { /* Notification delivery is best-effort. */ }
            }
            scheduleNext();
        }, delay);
    };
    scheduleNext();
}""")
private external fun scheduleBrowserReminder(enabled: Boolean, hour: Int, minute: Int, dueCount: Int, hebrew: Boolean)

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
    if (Number.isNaN(value.getTime())) return iso;
    return new Intl.DateTimeFormat(hebrewUi ? 'he-IL' : 'en-US', {
        weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', timeZone: 'UTC'
    }).format(value);
}""")
private external fun formatGregorianDate(iso: String, hebrewUi: Boolean): String

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
    readPresetCatalog()?.let { raw ->
        runCatching {
            val update = Json { ignoreUnknownKeys = true }.decodeFromString<WebPresetCatalogUpdate>(raw)
            WebPresetCatalog.applyVerifiedUpdate(update)
        }.onFailure { discardPresetCatalog() }
    }
    ComposeViewport(document.body!!) {
        val store = remember { LocalBrowserStore() }
        val cloudAccount = remember { SupabaseCloudAccount() }
        WebApp(store, cloudAccount)
    }
}
