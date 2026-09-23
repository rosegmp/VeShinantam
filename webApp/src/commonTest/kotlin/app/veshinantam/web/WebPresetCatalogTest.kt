package app.veshinantam.web

import app.veshinantam.shared.IsoDate
import app.veshinantam.shared.preset.SharedPresetCatalog
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WebPresetCatalogTest {
    @AfterTest
    fun clearUpdate() = WebPresetCatalog.clearVerifiedUpdate()

    @Test
    fun appliesACompleteValidatedCatalogUpdate() {
        val positions = SharedPresetCatalog.programs.associate { program ->
            program.id to program.units[(program.currentIndex + program.dailyQuantity).coerceAtMost(program.units.lastIndex)].english
        }

        WebPresetCatalog.applyVerifiedUpdate(
            WebPresetCatalogUpdate("2026.09.18-11", 11, "2026-09-18", positions),
        )

        assertEquals("2026.09.18-11", WebPresetCatalog.version)
        assertEquals(11, WebPresetCatalog.sequence)
        assertEquals(IsoDate(2026, 9, 18), WebPresetCatalog.positionAsOf)
        assertEquals(positions, WebPresetCatalog.programs.associate { it.id to it.currentReference.english })
    }

    @Test
    fun rejectsMissingOrUnknownPresetPositions() {
        val missing = SharedPresetCatalog.programs.drop(1).associate { it.id to it.currentReference.english }
        assertFailsWith<IllegalArgumentException> {
            WebPresetCatalog.applyVerifiedUpdate(WebPresetCatalogUpdate("bad", 11, "2026-09-18", missing))
        }

        val unknown = SharedPresetCatalog.programs.associate { it.id to it.currentReference.english }.toMutableMap()
        unknown[SharedPresetCatalog.programs.first().id] = "Not a real reference"
        assertFailsWith<IllegalArgumentException> {
            WebPresetCatalog.applyVerifiedUpdate(WebPresetCatalogUpdate("bad", 11, "2026-09-18", unknown))
        }
    }
}
