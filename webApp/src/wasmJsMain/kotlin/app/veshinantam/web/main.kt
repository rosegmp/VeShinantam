package app.veshinantam.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalWasmJsInterop

private const val StorageKey = "veshinantam.web.v1"

private class LocalBrowserStore(private val today: String) : BrowserStore {
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): WebAppState {
        val raw = readLocalStorage(StorageKey) ?: return WebAppState.sample(today).also(::save)
        return runCatching { json.decodeFromString<WebAppState>(raw) }.getOrElse { WebAppState.sample(today) }
    }

    override fun save(state: WebAppState) {
        writeLocalStorage(StorageKey, json.encodeToString(state))
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.localStorage.getItem(key)")
private external fun readLocalStorage(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key, value) => window.localStorage.setItem(key, value)")
private external fun writeLocalStorage(key: String, value: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => new Date().toISOString().slice(0, 10)")
private external fun currentIsoDate(): String

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val today = currentIsoDate()
    ComposeViewport(document.body!!) {
        WebApp(LocalBrowserStore(today), today)
    }
}
