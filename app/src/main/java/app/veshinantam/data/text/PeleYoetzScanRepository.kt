package app.veshinantam.data.text

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class PeleYoetzVolumeReference(val volume: Int, val day: Int) {
    companion object {
        private val pattern = Regex(
            """^Pele Yoetz, Volume ([12]), Day (\d+), Ot """,
            RegexOption.IGNORE_CASE,
        )

        fun parse(value: String): PeleYoetzVolumeReference? {
            val match = pattern.find(value.trim()) ?: return null
            val volume = match.groupValues[1].toInt()
            val parsedDay = match.groupValues[2].toInt()
            val day = if (volume == 2 && parsedDay in 1..245) parsedDay + 254 else parsedDay
            return PeleYoetzVolumeReference(
                volume = volume,
                day = day,
            ).takeIf { (it.volume == 1 && it.day in 1..254) || (it.volume == 2 && it.day in 255..499) }
        }
    }
}

class PeleYoetzDocumentRepository(context: Context) {
    private val appContext = context.applicationContext
    private val entries: List<String> by lazy {
        appContext.assets.open(FILE_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
            Json.decodeFromString<List<String>>(reader.readText()).also { require(it.size == 499) }
        }
    }

    fun read(day: Int): String? = entries.getOrNull(day - 1)

    private companion object {
        const val FILE_NAME = "pele_yoetz_schedule.json"
    }
}

data class PeleYoetzScanConfig(val uri: Uri)

class PeleYoetzScanRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(volume: Int): PeleYoetzScanConfig? = preferences.getString("uri_$volume", null)
        ?.let(Uri::parse)
        ?.let(::PeleYoetzScanConfig)

    suspend fun attach(volume: Int, uri: Uri): PeleYoetzScanConfig = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        require(pageCount(uri) > 0) { "The selected file is not a readable PDF." }
        preferences.edit().putString("uri_$volume", uri.toString()).apply()
        requireNotNull(read(volume))
    }

    suspend fun render(config: PeleYoetzScanConfig, pageIndex: Int): RenderedPdfPage = withContext(Dispatchers.IO) {
        appContext.contentResolver.openFileDescriptor(config.uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(pageIndex in 0 until renderer.pageCount) { "That page is outside the selected PDF." }
                renderer.openPage(pageIndex).use { page ->
                    val width = minOf(page.width, MAX_RENDER_WIDTH)
                    val height = (page.height.toDouble() * width / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                    }
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    RenderedPdfPage(bitmap, renderer.pageCount)
                }
            }
        } ?: error("The selected PDF is no longer available.")
    }

    private fun pageCount(uri: Uri): Int = appContext.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        PdfRenderer(descriptor).use(PdfRenderer::getPageCount)
    } ?: 0

    private companion object {
        const val PREFERENCES = "pele_yoetz_scans"
        const val MAX_RENDER_WIDTH = 1_600
    }
}
