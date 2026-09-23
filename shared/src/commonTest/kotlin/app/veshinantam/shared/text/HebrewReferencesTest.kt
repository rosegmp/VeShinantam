package app.veshinantam.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HebrewReferencesTest {
    @Test
    fun numeralsMatchReferenceSnapshots() {
        assertEquals(
            listOf("א", "ב", "ט", "י", "טו", "טז", "כט", "קד", "ת", "תרצז", "תתקצט"),
            listOf(1, 2, 9, 10, 15, 16, 29, 104, 400, 697, 999).map(HebrewNumerals::format),
        )
    }

    @Test
    fun numeralsRejectValuesOutsideTheSupportedReferenceRange() {
        assertFailsWith<IllegalArgumentException> { HebrewNumerals.format(0) }
        assertFailsWith<IllegalArgumentException> { HebrewNumerals.format(1000) }
    }

    @Test
    fun legacyReferencesNormalizeIdenticallyOnEveryPlatform() {
        assertEquals("ברכות דף ב.", HebrewReferenceFormatter.normalize("Berachos 2a", "ברכות 2."))
        assertEquals("יבמות דף קד:", HebrewReferenceFormatter.normalize("Yevamos 104b", "יבמות 104:"))
        assertEquals("כלים כט:ו", HebrewReferenceFormatter.normalize("Kelim 29:6", "כלים 29:6"))
        assertEquals("משנה ברורה חלק ב סימן טו", HebrewReferenceFormatter.normalize("Mishnah Berurah, chelek 2 siman 15", "משנה ברורה חלק 2 סימן 15"))
        assertEquals("מהדורת 2026", HebrewReferenceFormatter.normalize("Custom 2026", "מהדורת 2026"))
    }
}
