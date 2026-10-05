package app.veshinantam.data.text

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer

internal object PdfPageRasterizer {
    fun render(page: PdfRenderer.Page, pageCount: Int, requestedWidth: Int): RenderedPdfPage {
        val width = PdfRenderSizing.width(page.width, page.height, requestedWidth)
        val height = (page.height.toDouble() * width / page.width).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return RenderedPdfPage(bitmap, pageCount)
        } catch (error: Throwable) {
            bitmap.recycle()
            throw error
        }
    }
}
