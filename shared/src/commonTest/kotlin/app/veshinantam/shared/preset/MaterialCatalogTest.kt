package app.veshinantam.shared.preset

import kotlin.test.Test
import kotlin.test.assertEquals

class MaterialCatalogTest {
    @Test
    fun nachCanSpanBooksByChapter() {
        val units = MaterialCatalog.selectedUnits(
            SeferChoice.NACH, 0, 1,
            MaterialCatalog.nachUnits(MaterialCatalog.nach[0]).last(),
            MaterialCatalog.nachUnits(MaterialCatalog.nach[1]).first(),
        )
        assertEquals(listOf("Joshua 24", "Judges 1"), units.map { it.english })
        assertEquals(listOf("יהושע פרק כד", "שופטים פרק א"), units.map { it.hebrew })
        assertEquals(742, MaterialCatalog.nachYomiUnits.size)
        assertEquals("II Chronicles 36", MaterialCatalog.nachYomiUnits.last().english)
    }

    @Test
    fun selectedGemaraRangeCanCrossMasechtos() {
        val berachos = MaterialCatalog.gemara.first()
        val shabbos = MaterialCatalog.gemara[1]
        val start = MaterialCatalog.unitOptions(SeferChoice.GEMARA, berachos).last()
        val end = MaterialCatalog.unitOptions(SeferChoice.GEMARA, shabbos).first()

        val selected = MaterialCatalog.selectedUnits(
            choice = SeferChoice.GEMARA,
            fromSectionIndex = 0,
            toSectionIndex = 1,
            start = start,
            end = end,
        )

        assertEquals(listOf("Berachos 64", "Shabbos 2"), selected.map { it.english })
    }

    @Test
    fun selectedMishnahRangeUsesExactMishnayos() {
        val berachos = MaterialCatalog.mishnah.first()
        val options = MaterialCatalog.unitOptions(SeferChoice.MISHNAH, berachos)
        val selected = MaterialCatalog.selectedUnits(
            choice = SeferChoice.MISHNAH,
            fromSectionIndex = 0,
            toSectionIndex = 0,
            start = options.first { it.english == "Berachos 1:3" },
            end = options.first { it.english == "Berachos 1:5" },
        )

        assertEquals(listOf("Berachos 1:3", "Berachos 1:4", "Berachos 1:5"), selected.map { it.english })
    }

    @Test
    fun multipleRangesPreserveChosenOrderWithoutRepeatingOverlaps() {
        val berachos = MaterialCatalog.mishnah.first()
        val options = MaterialCatalog.unitOptions(SeferChoice.MISHNAH, berachos)
        val first = MaterialCatalog.selectedUnits(
            SeferChoice.MISHNAH, 0, 0,
            options.first { it.english == "Berachos 1:3" },
            options.first { it.english == "Berachos 1:4" },
        )
        val second = MaterialCatalog.selectedUnits(
            SeferChoice.MISHNAH, 0, 0,
            options.first { it.english == "Berachos 1:4" },
            options.first { it.english == "Berachos 1:5" },
        )

        assertEquals(
            listOf("Berachos 1:3", "Berachos 1:4", "Berachos 1:5"),
            MaterialCatalog.combineRanges(listOf(first, second)).map { it.english },
        )
        assertEquals(
            listOf("Berachos 1:4", "Berachos 1:5", "Berachos 1:3"),
            MaterialCatalog.combineRanges(listOf(second, first)).map { it.english },
        )
    }
}
