package app.veshinantam.shared.preset

import app.veshinantam.shared.CanonicalMaterialType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RemotePresetCatalogTest {
    private fun update(vararg patches: RemotePresetPatch, positions: Map<String, String> = emptyMap()) =
        RemotePresetCatalog(2, "2026.09.23-11", 11, "2026-09-23", positions, patches.toList())

    @Test
    fun addsACompleteProgramAndUpdatesBundledMetadata() {
        val programs = RemotePresetCatalogValidator.programs(update(
            RemotePresetPatch("daf-yomi-bavli", nameEnglish = "Daf Yomi"),
            RemotePresetPatch(
                id = "daily-sample",
                nameEnglish = "Daily Sample",
                nameHebrew = "לימוד יומי",
                materialType = "CUSTOM_UNIT",
                dailyQuantity = 1,
                selectedWeekdays = listOf(0, 1, 2, 3, 4),
                units = listOf(RemotePresetUnit("Sample 1", "דוגמה א"), RemotePresetUnit("Sample 2", "דוגמה ב")),
            ),
            positions = mapOf("daily-sample" to "Sample 1"),
        ))

        assertEquals(SharedPresetCatalog.programs.size + 1, programs.size)
        assertEquals("Daf Yomi", programs.first().nameEnglish)
        assertEquals(CanonicalMaterialType.CUSTOM_UNIT, programs.last().materialType)
        assertEquals("Sample 1", programs.last().currentReference.english)
    }

    @Test
    fun rejectsIncompleteAndInvalidProgramsWithoutChangingTheBundledCatalog() {
        assertFailsWith<IllegalArgumentException> {
            RemotePresetCatalogValidator.programs(update(RemotePresetPatch("new-preset", nameEnglish = "Incomplete")))
        }
        assertFailsWith<IllegalArgumentException> {
            RemotePresetCatalogValidator.programs(update(RemotePresetPatch("oraysa", selectedWeekdays = listOf(7))))
        }
        assertFailsWith<IllegalArgumentException> {
            RemotePresetCatalogValidator.programs(update(positions = mapOf("missing" to "Something")))
        }
        assertEquals(12, SharedPresetCatalog.programs.size)
    }
}
