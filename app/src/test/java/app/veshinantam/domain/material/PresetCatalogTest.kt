package app.veshinantam.domain.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetCatalogTest {
    @Test
    fun `bundled catalog exposes all first release programs at verified positions`() {
        assertEquals(
            listOf(
                "daf-yomi-bavli", "oraysa", "amud-yomi", "mishnah-yomis", "dirshu-mishnah-berurah",
                "daf-yomi-yerushalmi", "rambam-three-chapters", "chofetz-chaim", "tehillim-monthly",
                "hachzek-pele-yoetz",
                "kitzur-yomi",
            ),
            PresetCatalog.programs.map { it.id },
        )
        assertEquals(
            listOf(
                "Chullin 131", "Yevamos 104a", "Yoma 68a", "Kelim 29:6", "Mishnah Berurah, chelek 5 page 6a",
                "Yerushalmi Shevuos 13", "Rambam, Other Sources of Defilement 3",
                "Chofetz Chaim, Tziyurim 4-5", "Tehillim 119:97-176",
                "Pele Yoetz, Day 101",
                "Kitzur Shulchan Aruch 131:10-16",
            ),
            PresetCatalog.programs.map { it.currentReference.english },
        )
        assertTrue(PresetCatalog.programs.all { it.selectableStartingUnits.last() == it.currentReference })
    }

    @Test
    fun `preset references retain bilingual labels and Ashkenazi transliteration`() {
        val dafYomi = PresetCatalog.programs.first()
        assertTrue(dafYomi.units.any { it.english.startsWith("Berachos ") && it.hebrew.startsWith("ברכות ") })
        assertTrue(dafYomi.units.any { it.english.startsWith("Shabbos ") && it.hebrew.startsWith("שבת ") })
    }

    @Test
    fun `shas presets use special Vilna pagination for small masechtos`() {
        val dafUnits = PresetCatalog.programs.first { it.id == "daf-yomi-bavli" }.units.map { it.english }
        val amudUnits = PresetCatalog.programs.first { it.id == "amud-yomi" }.units.map { it.english }

        assertTrue("Kinnim 22" in dafUnits)
        assertTrue("Middos 37" in dafUnits)
        assertTrue("Kinnim 25a" in amudUnits)
        assertTrue("Kinnim 25b" !in amudUnits)
        assertTrue("Tamid 25b" in amudUnits)
        assertTrue("Middos 37b" in amudUnits)
    }

    @Test
    fun `Oraysa uses its program order rather than standard Daf Yomi order`() {
        val units = PresetCatalog.programs.first { it.id == "oraysa" }.units.map { it.english }

        assertTrue(units.none { it.startsWith("Shekalim ") })
        assertTrue(units.indexOf("Pesachim 121a") < units.indexOf("Rosh Hashanah 2a"))
        assertTrue(units.indexOf("Rosh Hashanah 35a") < units.indexOf("Yoma 2a"))
        assertTrue(units.indexOf("Beitzah 40a") < units.indexOf("Megillah 2a"))
        assertTrue(units.indexOf("Megillah 32a") < units.indexOf("Taanis 2a"))
        assertTrue(units.indexOf("Chagigah 27a") < units.indexOf("Yevamos 2a"))
    }

    @Test
    fun `earlier positions resolve to their original program dates`() {
        val dafYomi = PresetCatalog.programs.first { it.id == "daf-yomi-bavli" }
        val oraysa = PresetCatalog.programs.first { it.id == "oraysa" }
        val mishnahYomis = PresetCatalog.programs.first { it.id == "mishnah-yomis" }

        assertEquals(PresetCatalog.positionAsOf, PresetCatalog.scheduledDate(dafYomi, dafYomi.currentIndex))
        assertEquals(PresetCatalog.positionAsOf.minusDays(1), PresetCatalog.scheduledDate(dafYomi, dafYomi.currentIndex - 1))
        assertEquals(PresetCatalog.positionAsOf.minusDays(5), PresetCatalog.scheduledDate(oraysa, oraysa.currentIndex - 3))
        assertEquals(PresetCatalog.positionAsOf.minusDays(1), PresetCatalog.scheduledDate(mishnahYomis, mishnahYomis.currentIndex - 1))
        assertEquals(PresetCatalog.positionAsOf.minusDays(1), PresetCatalog.scheduledDate(mishnahYomis, mishnahYomis.currentIndex - 2))
    }

    @Test
    fun `current masechta option resolves its first unit and original start date`() {
        val oraysa = PresetCatalog.programs.first { it.id == "oraysa" }
        val startIndex = requireNotNull(PresetCatalog.currentMasechtaStartIndex(oraysa))

        assertEquals("Yevamos 2a", oraysa.units[startIndex].english)
        assertEquals(java.time.LocalDate.of(2025, 11, 26), PresetCatalog.scheduledDate(oraysa, startIndex))
        assertEquals(
            null,
            PresetCatalog.currentMasechtaStartIndex(PresetCatalog.programs.first { it.id == "tehillim-monthly" }),
        )
    }

    @Test
    fun `Dirshu preset is one two-sided-catalog page per weekday`() {
        val dirshu = PresetCatalog.programs.first { it.id == "dirshu-mishnah-berurah" }

        assertEquals(app.veshinantam.domain.model.MaterialType.PAGE, dirshu.materialType)
        assertEquals(1, dirshu.dailyQuantity)
        assertTrue(dirshu.units.any { it.english.endsWith("page 2a") })
        assertTrue(dirshu.units.any { it.english.endsWith("page 2b") })
    }

    @Test
    fun `new daily programs use their standard pace and cycle structure`() {
        val yerushalmi = PresetCatalog.programs.first { it.id == "daf-yomi-yerushalmi" }
        val rambam = PresetCatalog.programs.first { it.id == "rambam-three-chapters" }
        val chofetzChaim = PresetCatalog.programs.first { it.id == "chofetz-chaim" }
        val tehillim = PresetCatalog.programs.first { it.id == "tehillim-monthly" }
        val peleYoetz = PresetCatalog.programs.first { it.id == "hachzek-pele-yoetz" }
        val kitzurYomi = PresetCatalog.programs.first { it.id == "kitzur-yomi" }

        assertEquals(1, yerushalmi.dailyQuantity)
        assertEquals(1554, yerushalmi.units.size)
        assertTrue(PresetCatalog.positionAsOf.plusDays(13) in yerushalmi.excludedDates)
        assertEquals(3, rambam.dailyQuantity)
        assertEquals(1017, rambam.units.size)
        assertTrue(rambam.units.any { it.english.endsWith("Leavened and Unleavened Bread 8-9") })
        assertEquals(1, chofetzChaim.dailyQuantity)
        assertEquals("חפץ חיים, ציורים ד–ה", chofetzChaim.currentReference.hebrew)
        assertEquals(29, tehillim.units.size)
        assertEquals("Tehillim 140-150", tehillim.units.last().english)
        assertEquals("תהילים קיט:צז–קעו", tehillim.currentReference.hebrew)
        assertEquals(1, peleYoetz.dailyQuantity)
        assertEquals(245, peleYoetz.units.size)
        assertEquals("פלא יועץ, יום קא", peleYoetz.currentReference.hebrew)
        assertEquals(java.time.LocalDate.of(2026, 5, 12), PresetCatalog.scheduledDate(peleYoetz, 0))
        assertEquals(java.time.LocalDate.of(2026, 9, 8), PresetCatalog.scheduledDate(peleYoetz, 100))
        assertTrue(java.time.DayOfWeek.SATURDAY !in peleYoetz.selectedWeekdays)
        assertTrue(java.time.LocalDate.of(2026, 9, 21) in peleYoetz.excludedDates)
        assertEquals(354, kitzurYomi.units.size)
        assertEquals("Kitzur Shulchan Aruch 1:1-4", kitzurYomi.units.first().english)
        assertEquals("קיצור שולחן ערוך קלא:י-טז", kitzurYomi.currentReference.hebrew)
        assertEquals("Kitzur Shulchan Aruch 100:17-E", kitzurYomi.units.last().english)
        assertEquals(java.time.LocalDate.of(2025, 10, 16), PresetCatalog.scheduledDate(kitzurYomi, 0))
        assertEquals(java.time.LocalDate.of(2026, 9, 8), PresetCatalog.scheduledDate(kitzurYomi, kitzurYomi.currentIndex))
    }
}
