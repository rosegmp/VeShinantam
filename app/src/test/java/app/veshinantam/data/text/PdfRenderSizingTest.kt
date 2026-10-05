package app.veshinantam.data.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfRenderSizingTest {
    @Test
    fun rendersAtRequestedScreenWidthInsteadOfPdfPointWidth() {
        assertEquals(1080, PdfRenderSizing.width(612, 792, 1080))
        assertEquals(2000, PdfRenderSizing.width(612, 792, 2000))
    }

    @Test
    fun capsZoomedPagesToMemoryBudget() {
        val width = PdfRenderSizing.width(612, 792, 10_000)
        val height = (792.0 * width / 612).toInt()
        assertTrue(width <= 4096)
        assertTrue(width.toLong() * height <= PdfRenderSizing.MAX_BITMAP_PIXELS)
    }
}
