package app.veshinantam.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SefariaTextTest {
    @Test
    fun mapsSupportedCatalogReferences() {
        fun request(reference: String, type: String, preset: String? = null) =
            assertIs<SefariaTextLookup.Available>(SefariaReferenceMapper.lookup(reference, type, preset)).request

        assertEquals("Chullin 133", request("Chullin 133", "DAF", "daf-yomi-bavli").reference)
        assertEquals("Mishnah Kelim 30:2", request("Kelim 30:2", "MISHNAH", "mishnah-yomis").reference)
        assertEquals("Mishneh Torah, Other Sources of Defilement 9", request("Rambam, Other Sources of Defilement 9", "PEREK").reference)
        assertEquals("Psalms 120-134", request("Tehillim 120-134", "PEREK").reference)
        assertEquals("Pele Yoetz 103", request("Pele Yoetz, Day 103", "CUSTOM_UNIT").reference)
        assertEquals("Kitzur Shulchan Arukh 133:1-8", request("Kitzur Shulchan Aruch 133:1-8", "CUSTOM_UNIT").reference)
        assertEquals("Mishnah Berurah 1", request("Mishnah Berurah, chelek 1 siman 1", "SIMAN").reference)
        assertEquals("Mishnah Berurah 1:3", request("Mishnah Berurah, chelek 1 siman 1 seif 3", "SEIF").reference)
        assertEquals("Mishnah Berakhot 1", request("Berachos 1", "PEREK").reference)
        assertEquals("Mishnah Berakhot 1", request("Berachos perek 1", "PEREK").reference)
        assertEquals("Mishnah Oholot 1:1", request("Ohalos 1:1", "MISHNAH", "mishnah-yomis").reference)
        assertEquals("Mishnah Oholot 1:1", request("Mishnah Ohalos 1:1", "MISHNAH").reference)
        assertEquals("Mishnah Avot 1:1", request("Avos 1:1", "MISHNAH").reference)
    }

    @Test
    fun rejectsReferencesThatCannotBeMappedSafely() {
        assertIs<SefariaTextLookup.Unavailable>(SefariaReferenceMapper.lookup("Yerushalmi Yevamos 33", "DAF", "yerushalmi-yomi-schottenstein"))
        assertIs<SefariaTextLookup.Unavailable>(SefariaReferenceMapper.lookup("Mishnah Berurah, chelek 5 page 7a", "PAGE", "dirshu-mishnah-berurah"))
        assertIs<SefariaTextLookup.Unavailable>(SefariaReferenceMapper.lookup("Chofetz Chaim, Tziyurim 8-9", "CUSTOM_UNIT", "chofetz-chaim"))
        assertIs<SefariaTextLookup.Unavailable>(
            SefariaReferenceMapper.lookup(
                "Pele Yoetz, Volume 1, Day 1, Ot Alef - אהבה להקדוש ברוך הוא",
                "CUSTOM_UNIT",
                "hachzek-pele-yoetz",
            ),
        )
        assertIs<SefariaTextLookup.Unavailable>(
            SefariaReferenceMapper.lookup(
                "Pele Yoetz, Volume 2, Day 357, Ot Tet - וכן יש לזהר מלגע בנבלה",
                "CUSTOM_UNIT",
                "hachzek-pele-yoetz",
            ),
        )
        assertIs<SefariaTextLookup.Unavailable>(SefariaReferenceMapper.lookup("Read chapter one", "CUSTOM_UNIT", null))
    }

    @Test
    fun parsesBilingualTextAndLicenseMetadata() {
        val content = SefariaTextParser.parse(
            """{"ref":"Psalms 1","heRef":"תהילים א׳","versions":[
                {"language":"he","versionTitle":"Open Hebrew","license":"Public Domain","versionSource":"https://example.test/he","text":["א",["ב"]]},
                {"language":"en","versionTitle":"Open English","license":"CC-BY","text":["One","Two"]}
            ]}""",
        )
        assertEquals(listOf("א", "ב"), content.hebrew?.segments)
        assertEquals(listOf("One", "Two"), content.english?.segments)
        assertTrue(content.mayCache)
        assertFalse(content.copy(versions = content.versions.map { it.copy(license = "unknown") }).mayCache)
    }
}
