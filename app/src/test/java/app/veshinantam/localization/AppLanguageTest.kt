package app.veshinantam.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `Hebrew language tags select Hebrew`() {
        assertEquals(AppLanguage.HEBREW, AppLanguage.fromTag("he"))
        assertEquals(AppLanguage.HEBREW, AppLanguage.fromTag("he-IL"))
    }

    @Test
    fun `English and unsupported tags use English`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(null))
    }

    @Test
    fun `Hebrew numbers use letters and avoid divine-name spellings`() {
        assertEquals("ב", HebrewNumerals.format(2))
        assertEquals("טו", HebrewNumerals.format(15))
        assertEquals("טז", HebrewNumerals.format(16))
        assertEquals("קלא", HebrewNumerals.format(131))
        assertEquals("תרצז", HebrewNumerals.format(697))
    }

    @Test
    fun `legacy Gemara references gain Hebrew daf wording and numbering`() {
        assertEquals("ברכות דף ב.", HebrewReferenceFormatter.normalize("Berachos 2a", "ברכות 2."))
        assertEquals("כלים כט:ו", HebrewReferenceFormatter.normalize("Kelim 29:6", "כלים 29:6"))
    }

    @Test
    fun `sefarim preference is independent of interface language`() {
        assertEquals(
            "\u2067ברכות דף ב.\u2069",
            SefarimDisplay.label("Berachos 2a", "ברכות 2.", SefarimLanguage.HEBREW, AppLanguage.ENGLISH),
        )
        assertEquals(
            "Berachos 2a / \u2067ברכות דף ב.\u2069",
            SefarimDisplay.label("Berachos 2a", "ברכות 2.", SefarimLanguage.BOTH, AppLanguage.ENGLISH),
        )
        assertEquals(
            "ברכות דף ב. / \u2066Berachos 2a\u2069",
            SefarimDisplay.label("Berachos 2a", "ברכות 2.", SefarimLanguage.BOTH, AppLanguage.HEBREW),
        )
    }

    @Test
    fun `Hebrew amud punctuation is isolated inside an English interface`() {
        assertEquals(
            "\u2067יבמות דף קד.\u2069",
            SefarimDisplay.label("Yevamos 104a", "יבמות קד.", SefarimLanguage.HEBREW, AppLanguage.ENGLISH),
        )
        assertEquals(
            "\u2067יבמות דף קד:\u2069",
            SefarimDisplay.label("Yevamos 104b", "יבמות קד:", SefarimLanguage.HEBREW, AppLanguage.ENGLISH),
        )
    }

    @Test
    fun `user entered names are isolated according to their actual script`() {
        assertEquals("\u2067תוכנית אישית\u2069", BidiText.isolateForUi("תוכנית אישית", AppLanguage.ENGLISH))
        assertEquals("\u2066Personal schedule\u2069", BidiText.isolateForUi("Personal schedule", AppLanguage.HEBREW))
        assertEquals("Personal schedule", BidiText.isolateForUi("Personal schedule", AppLanguage.ENGLISH))
    }

    @Test
    fun `primary calendar supports Gregorian and Hebrew independently`() {
        assertEquals(listOf(PrimaryCalendar.GREGORIAN, PrimaryCalendar.HEBREW), PrimaryCalendar.entries)
    }
}
