package app.veshinantam.shared.preset

import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlyTehillimTest {
    @Test
    fun followsTheHebrewMonthTable() {
        val expected = listOf(
            "1-9", "10-17", "18-22", "23-28", "29-34", "35-38", "39-43", "44-48", "49-54", "55-59",
            "60-65", "66-68", "69-71", "72-76", "77-78", "79-82", "83-87", "88-89", "90-96", "97-103",
            "104-105", "106-107", "108-112", "113-118", "119:1-96", "119:97-176", "120-134", "135-139",
            "140-144", "145-150",
        )
        assertEquals(expected.map { "Tehillim $it" }, (1..30).map { MaterialCatalog.monthlyTehillimForDay(it, 30).english })
        assertEquals("Tehillim 140-150", MaterialCatalog.monthlyTehillimForDay(29, 29).english)
        assertEquals("תהילים קמ–קנ", MaterialCatalog.monthlyTehillimForDay(29, 29).hebrew)
    }
}
