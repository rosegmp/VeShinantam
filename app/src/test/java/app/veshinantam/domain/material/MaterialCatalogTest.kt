package app.veshinantam.domain.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MaterialCatalogTest {
    @Test
    fun `gemara amud range crosses selected masechtos in canonical order`() {
        val selected = listOf(MaterialCatalog.gemara[0], MaterialCatalog.gemara[1])
        val units = MaterialCatalog.gemaraUnits(selected, startDaf = 63, endDaf = 3, unit = GemaraUnit.AMUD)

        assertEquals(listOf("Berachos 63a", "Berachos 63b", "Berachos 64a", "Shabbos 2a", "Shabbos 2b", "Shabbos 3a", "Shabbos 3b"), units.map { it.english })
        assertEquals("ברכות דף סג.", units.first().hebrew)
        assertEquals("שבת דף ג:", units.last().hebrew)
    }

    @Test
    fun `gemara final amud follows each masechta endpoint`() {
        val berachos = MaterialCatalog.gemara.first { it.english == "Berachos" }
        val shabbos = MaterialCatalog.gemara.first { it.english == "Shabbos" }

        assertEquals(listOf("Berachos 64a"), MaterialCatalog.gemaraUnits(listOf(berachos), 64, 64, GemaraUnit.AMUD).map { it.english })
        assertEquals(listOf("Shabbos 157a", "Shabbos 157b"), MaterialCatalog.gemaraUnits(listOf(shabbos), 157, 157, GemaraUnit.AMUD).map { it.english })
    }

    @Test
    fun `mishnah range validates first and last masechta bounds`() {
        assertThrows(IllegalArgumentException::class.java) {
            MaterialCatalog.mishnahPerakim(listOf(MaterialCatalog.mishnah.first()), 1, 10)
        }
    }

    @Test
    fun `mishnah units use actual mishnayos per perek`() {
        val berachos = MaterialCatalog.mishnah.first()
        val units = MaterialCatalog.mishnahUnits(berachos, MishnahUnit.MISHNAH)

        assertEquals(57, units.size)
        assertEquals("Berachos 1:1", units.first().english)
        assertEquals("Berachos 9:5", units.last().english)
        assertEquals("ברכות ט:ה", units.last().hebrew)
    }

    @Test
    fun `mishnah berurah chelakim cover every siman once`() {
        val values = MaterialCatalog.mishnahBerurahChelakim.flatMap { it.toList() }
        assertEquals((1..697).toList(), values)
    }

    @Test
    fun `mishnah berurah supports page seif and siman units`() {
        val pages = MaterialCatalog.mishnahBerurahUnitOptions(2, MishnahBerurahUnit.PAGE)
        val seifim = MaterialCatalog.mishnahBerurahUnitOptions(2, MishnahBerurahUnit.SEIF)
        val simanim = MaterialCatalog.mishnahBerurahUnitOptions(2, MishnahBerurahUnit.SIMAN)

        assertEquals(300, pages.size)
        assertEquals("Mishnah Berurah, chelek 2 page 2a", pages.first().english)
        assertEquals("Mishnah Berurah, chelek 2 page 151b", pages.last().english)
        assertEquals("משנה ברורה חלק ב עמוד קנא:", pages.last().hebrew)
        assertEquals(MaterialStructureData.mishnahBerurahSeifimPerSiman.slice(127..240).sum(), seifim.size)
        assertEquals(114, simanim.size)
    }

    @Test
    fun `mishnah berurah unit counts are distinct and complete`() {
        assertEquals(697, MaterialStructureData.mishnahBerurahSeifimPerSiman.size)
        val pageCount = MaterialCatalog.mishnahBerurahUnitOptions(1, MishnahBerurahUnit.PAGE).size
        val seifCount = MaterialCatalog.mishnahBerurahUnitOptions(1, MishnahBerurahUnit.SEIF).size
        val simanCount = MaterialCatalog.mishnahBerurahUnitOptions(1, MishnahBerurahUnit.SIMAN).size
        assertEquals(296, pageCount)
        assertEquals(127, simanCount)
        assertEquals(false, pageCount == simanCount || seifCount == simanCount)
    }

    @Test
    fun `Yerushalmi uses Vilna pagination and Ashkenazi names`() {
        val berachos = MaterialCatalog.yerushalmi.first()
        val shevuos = MaterialCatalog.yerushalmi.first { it.english == "Shevuos" }

        assertEquals(68, MaterialCatalog.yerushalmiUnits(berachos).size)
        assertEquals("Yerushalmi Berachos 1", MaterialCatalog.yerushalmiUnits(berachos).first().english)
        assertEquals("ירושלמי שבועות דף מד.", MaterialCatalog.yerushalmiUnits(shevuos).last().hebrew)
    }

    @Test
    fun `Rambam Chofetz Chaim and Tehillim expose complete custom ranges`() {
        assertEquals(88, MaterialCatalog.rambam.size)
        assertEquals(20, MaterialCatalog.rambamUnits(MaterialCatalog.rambam.first { it.english == "Other Sources of Defilement" }).size)
        assertEquals("Chofetz Chaim, Preface 1-4", MaterialCatalog.chofetzChaimUnits.first().english)
        assertEquals("Chofetz Chaim, Tziyurim 10-11", MaterialCatalog.chofetzChaimUnits.last().english)
        assertEquals(150, MaterialCatalog.tehillimUnits.size)
        assertEquals("תהילים פרק קנ", MaterialCatalog.tehillimUnits.last().hebrew)
    }
}
