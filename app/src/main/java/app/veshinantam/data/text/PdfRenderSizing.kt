package app.veshinantam.data.text

import kotlin.math.sqrt

/** Limits full-page bitmaps while allowing enough pixels for a sharp screen-sized view. */
internal object PdfRenderSizing {
    const val MAX_BITMAP_PIXELS = 8_000_000
    private const val MAX_RENDER_WIDTH = 4_096

    fun width(pageWidth: Int, pageHeight: Int, requestedWidth: Int): Int {
        require(pageWidth > 0 && pageHeight > 0)
        val maxWidthByPixels = sqrt(MAX_BITMAP_PIXELS.toDouble() * pageWidth / pageHeight)
            .toInt()
            .coerceAtLeast(1)
        return minOf(requestedWidth.coerceAtLeast(1), MAX_RENDER_WIDTH, maxWidthByPixels)
    }
}
