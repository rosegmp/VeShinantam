package app.veshinantam.shared.preset

import kotlin.test.Test
import kotlin.test.assertEquals

class MaterialCatalogTest {
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
}
