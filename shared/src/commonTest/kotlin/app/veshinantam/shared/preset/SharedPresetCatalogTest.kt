package app.veshinantam.shared.preset

import app.veshinantam.shared.IsoDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedPresetCatalogTest {
    @Test
    fun exposesEveryAndroidPresetAtItsVerifiedPosition() {
        assertEquals(
            listOf(
                "daf-yomi-bavli", "oraysa", "amud-yomi", "mishnah-yomis", "dirshu-mishnah-berurah",
                "yerushalmi-yomi-vilna", "yerushalmi-yomi-schottenstein", "rambam-three-chapters",
                "chofetz-chaim", "tehillim-monthly", "hachzek-pele-yoetz", "kitzur-yomi",
            ),
            SharedPresetCatalog.programs.map { it.id },
        )
        assertEquals(
            listOf(
                "Chullin 133", "Yevamos 105a", "Yoma 69a", "Kelim 30:2",
                "Mishnah Berurah, chelek 5 page 7a", "Yerushalmi Shevuos 15",
                "Yerushalmi Yevamos 33", "Rambam, Other Sources of Defilement 9",
                "Chofetz Chaim, Tziyurim 8-9", "Tehillim 120-134", "Pele Yoetz, Day 103",
                "Kitzur Shulchan Aruch 133:1-8",
            ),
            SharedPresetCatalog.programs.map { it.currentReference.english },
        )
    }

    @Test
    fun retainsExactCycleStructuresAndBilingualReferences() {
        val vilna = SharedPresetCatalog.programs.first { it.id == "yerushalmi-yomi-vilna" }
        val schottenstein = SharedPresetCatalog.programs.first { it.id == "yerushalmi-yomi-schottenstein" }
        val rambam = SharedPresetCatalog.programs.first { it.id == "rambam-three-chapters" }
        val kitzur = SharedPresetCatalog.programs.first { it.id == "kitzur-yomi" }

        assertEquals(1554, vilna.units.size)
        assertEquals(2094, schottenstein.units.size)
        assertEquals(1017, rambam.units.size)
        assertEquals(354, kitzur.units.size)
        assertEquals("קיצור שולחן ערוך קלג:א-ח", kitzur.currentReference.hebrew)
        assertTrue(rambam.units.any { it.english.endsWith("Leavened and Unleavened Bread 8-9") })
        assertFalse(schottenstein.units.any { it.hebrew.isBlank() })
    }

    @Test
    fun earlierPositionsKeepTheirOriginalProgramDates() {
        val oraysa = SharedPresetCatalog.programs.first { it.id == "oraysa" }
        val masechtaStart = requireNotNull(SharedPresetCatalog.currentMasechtaStartIndex(oraysa))

        assertEquals("Yevamos 2a", oraysa.units[masechtaStart].english)
        assertEquals(IsoDate(2025, 11, 26), SharedPresetCatalog.scheduledDate(oraysa, masechtaStart))
        assertEquals(SharedPresetCatalog.positionAsOf, SharedPresetCatalog.scheduledDate(oraysa, oraysa.currentIndex))
    }
}
