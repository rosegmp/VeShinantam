package app.veshinantam.data.text

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MishnahBerurahPageReference(
    val chelek: Int,
    val daf: Int,
    val side: Int,
) {
    val ordinal: Int get() = (daf - FIRST_DAFS[chelek - 1]) * 2 + side

    companion object {
        private val pattern = Regex(
            """Mishnah Berurah,?\s+chelek\s+([1-6])\s+page\s+(\d+)\s*([ab])""",
            RegexOption.IGNORE_CASE,
        )
        private val FIRST_DAFS = listOf(4, 2, 2, 196, 2, 2)

        fun parse(value: String): MishnahBerurahPageReference? {
            val match = pattern.find(value.trim()) ?: return null
            val chelek = match.groupValues[1].toInt()
            val daf = match.groupValues[2].toInt()
            val side = if (match.groupValues[3].equals("a", ignoreCase = true)) 0 else 1
            if (daf < FIRST_DAFS[chelek - 1]) return null
            return MishnahBerurahPageReference(chelek, daf, side)
        }
    }
}

data class MishnahBerurahScanConfig(
    val uri: Uri,
    val anchorOrdinal: Int,
    val anchorPdfPage: Int,
    val calibrated: Boolean,
)

data class RenderedPdfPage(val bitmap: Bitmap, val pageCount: Int)

class MishnahBerurahScanRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(chelek: Int): MishnahBerurahScanConfig? {
        val rawUri = preferences.getString("uri_$chelek", null) ?: return null
        return MishnahBerurahScanConfig(
            uri = Uri.parse(rawUri),
            anchorOrdinal = preferences.getInt("anchor_ordinal_$chelek", 0),
            anchorPdfPage = preferences.getInt("anchor_pdf_page_$chelek", 0),
            calibrated = preferences.getBoolean("calibrated_$chelek", false),
        )
    }

    suspend fun attach(chelek: Int, uri: Uri, anchorOrdinal: Int): MishnahBerurahScanConfig = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        require(pageCount(uri) > 0) { "The selected file is not a readable PDF." }
        preferences.edit()
            .putString("uri_$chelek", uri.toString())
            .putInt("anchor_ordinal_$chelek", anchorOrdinal)
            .putInt("anchor_pdf_page_$chelek", 0)
            .putBoolean("calibrated_$chelek", false)
            .apply()
        requireNotNull(read(chelek))
    }

    fun resolvedPage(reference: MishnahBerurahPageReference, config: MishnahBerurahScanConfig): Int =
        config.anchorPdfPage + reference.ordinal - config.anchorOrdinal

    fun saveAlignment(reference: MishnahBerurahPageReference, pdfPage: Int): MishnahBerurahScanConfig {
        preferences.edit()
            .putInt("anchor_ordinal_${reference.chelek}", reference.ordinal)
            .putInt("anchor_pdf_page_${reference.chelek}", pdfPage)
            .putBoolean("calibrated_${reference.chelek}", true)
            .apply()
        return requireNotNull(read(reference.chelek))
    }

    suspend fun render(config: MishnahBerurahScanConfig, pageIndex: Int): RenderedPdfPage = withContext(Dispatchers.IO) {
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
        const val PREFERENCES = "mishnah_berurah_scans"
        const val MAX_RENDER_WIDTH = 1_600
    }
}
