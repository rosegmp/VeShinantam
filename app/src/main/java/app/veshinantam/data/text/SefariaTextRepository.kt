package app.veshinantam.data.text

import android.content.Context
import app.veshinantam.shared.text.SefariaTextContent
import app.veshinantam.shared.text.SefariaTextParser
import app.veshinantam.shared.text.SefariaTextRequest
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SefariaTextResult {
    data class Ready(val content: SefariaTextContent, val fromCache: Boolean) : SefariaTextResult
    data class Error(val message: String) : SefariaTextResult
}

class SefariaTextRepository(context: Context) {
    private val cacheDirectory = context.applicationContext.filesDir.resolve("sefaria-text-cache")

    suspend fun load(request: SefariaTextRequest): SefariaTextResult = withContext(Dispatchers.IO) {
        val cache = cacheDirectory.resolve("${sha256(request.cacheKey)}.json")
        if (cache.isFile && cache.length() in 1..MAX_RESPONSE_BYTES) {
            runCatching { SefariaTextParser.parse(cache.readText()) }.getOrNull()?.let {
                cache.setLastModified(System.currentTimeMillis())
                return@withContext SefariaTextResult.Ready(it, fromCache = true)
            }
            cache.delete()
        }

        runCatching {
            val raw = fetch(request)
            val content = SefariaTextParser.parse(raw)
            if (content.mayCache) {
                cacheDirectory.mkdirs()
                cache.writeText(raw)
                pruneCache()
            }
            SefariaTextResult.Ready(content, fromCache = false)
        }.getOrElse { error ->
            SefariaTextResult.Error(error.message ?: "The text could not be loaded.")
        }
    }

    private fun fetch(request: SefariaTextRequest): String {
        val reference = encode(request.reference)
        val versions = request.versions.joinToString("") { "&version=${encode(it)}" }
        val url = URL("https://www.sefaria.org/api/v3/texts/$reference?return_format=text_only$versions")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VeShinantam/0.1 text-reader")
        }
        return try {
            require(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Sefaria returned ${connection.responseCode}."
            }
            val declaredLength = connection.contentLengthLong
            require(declaredLength < 0 || declaredLength <= MAX_RESPONSE_BYTES) { "The passage is too large to display." }
            connection.inputStream.use { input ->
                val result = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(result.size() + count <= MAX_RESPONSE_BYTES) { "The passage is too large to display." }
                    result.write(buffer, 0, count)
                }
                result.toString(Charsets.UTF_8.name())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun pruneCache() {
        cacheDirectory.listFiles()
            ?.filter(File::isFile)
            ?.sortedByDescending(File::lastModified)
            ?.drop(MAX_CACHED_PASSAGES)
            ?.forEach(File::delete)
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 2_000_000L
        const val MAX_CACHED_PASSAGES = 10
    }
}
