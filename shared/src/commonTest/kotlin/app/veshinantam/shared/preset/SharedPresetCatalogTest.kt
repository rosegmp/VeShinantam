package app.veshinantam.shared.preset

import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.SharedMaterialUnit
import app.veshinantam.shared.SharedScheduleEngine
import app.veshinantam.shared.SharedScheduleRules
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
                "Chofetz Chaim, Tziyurim 8-9", "Tehillim 120-134",
                "Pele Yoetz, Volume 2, Day 357, Ot Tet - וכן יש לזהר מלגע בנבלה וכל דבר טמא ושלא לילך בין המתים.",
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
        val peleYoetz = SharedPresetCatalog.programs.first { it.id == "hachzek-pele-yoetz" }

        assertEquals(1554, vilna.units.size)
        assertEquals(2094, schottenstein.units.size)
        assertEquals(1017, rambam.units.size)
        assertEquals(354, kitzur.units.size)
        assertEquals("קיצור שולחן ערוך קלג:א-ח", kitzur.currentReference.hebrew)
        assertTrue(rambam.units.any { it.english.endsWith("Leavened and Unleavened Bread 8-9") })
        assertFalse(schottenstein.units.any { it.hebrew.isBlank() })
        assertEquals(499, peleYoetz.units.size)
        assertEquals("פלא יועץ, חלק ב, יום שנז, אות ט - וכן יש לזהר מלגע בנבלה וכל דבר טמא ושלא לילך בין המתים.", peleYoetz.currentReference.hebrew)
        assertEquals("Pele Yoetz, Volume 1, Day 1, Ot Alef - אהבה להקדוש ברוך הוא - אהבה להקדוש ברוך הוא אין מדה טובה", peleYoetz.units.first().english)
        assertEquals("Pele Yoetz, Volume 2, Day 255, Ot Vav - אות ו: ותרנות - ותרנות היא מדה טובה, כשהוא ותרן בממונו לדבר", peleYoetz.units[254].english)
        assertTrue(peleYoetz.units.none { "page" in it.english.lowercase() })
    }

    @Test
    fun earlierPositionsKeepTheirOriginalProgramDates() {
        val oraysa = SharedPresetCatalog.programs.first { it.id == "oraysa" }
        val masechtaStart = requireNotNull(SharedPresetCatalog.currentMasechtaStartIndex(oraysa))

        assertEquals("Yevamos 2a", oraysa.units[masechtaStart].english)
        assertEquals(IsoDate(2025, 11, 26), SharedPresetCatalog.scheduledDate(oraysa, masechtaStart))
        assertEquals(SharedPresetCatalog.positionAsOf, SharedPresetCatalog.scheduledDate(oraysa, oraysa.currentIndex))
    }

    @Test
    fun mishnahYomisDefaultsToTwoLearningAssignmentsWithoutAutomaticChazarah() {
        val bundled = SharedPresetCatalog.programs.first { it.id == "mishnah-yomis" }
        val program = SharedPresetCatalog.programAtDate(bundled, IsoDate(2026, 9, 16))
        val units = program.units.subList(program.currentIndex, program.currentIndex + 4).mapIndexed { index, reference ->
            SharedMaterialUnit("mishnah-$index", index, reference.english, reference.hebrew)
        }
        val learning = SharedScheduleEngine().generateByDailyQuantity(
            units = units,
            startDate = IsoDate(2026, 9, 16),
            unitsPerDay = program.dailyQuantity,
            rules = SharedScheduleRules(program.selectedWeekdays),
        )

        assertEquals("Ohalos 2:2", program.currentReference.english)
        assertEquals("Ohalos 2:3", program.units[program.currentIndex + 1].english)
        assertEquals(2, learning.count { it.plannedDate == IsoDate(2026, 9, 16) })
        assertEquals(emptyList(), SharedPresetCatalog.defaultAdditionalChazarahOffsets(program.id))
        assertEquals(emptyList(), SharedPresetCatalog.defaultAdditionalChazarahOffsets("remote-preset"))
    }
}
